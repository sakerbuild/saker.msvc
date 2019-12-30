#include <jni.h>
#include <Windows.h>
#include <saker_msvc_proc_NativeProcess.h>
#include <string>

static std::wstring Java_To_WStr(JNIEnv *env, jstring string) {
    std::wstring value;

    const jchar *raw = env->GetStringChars(string, 0);
    jsize len = env->GetStringLength(string);

    value.assign(raw, raw + len);

    env->ReleaseStringChars(string, raw);

    return value;
}

static void javaException(JNIEnv* env, const char* type, const char* message) {
	jclass jc = env->FindClass(type);
	env->ThrowNew(jc, message);
}
static void failureType(JNIEnv* env, const char* exceptiontype, const char* function, DWORD error){
	char msg[256];
	sprintf_s(msg, "%s: %d", function, error);
	javaException(env, exceptiontype, msg);
}
static void failure(JNIEnv* env, const char* function, DWORD error){
	failureType(env, "java/io/IOException", function, error);
}
static void interruptException(JNIEnv* env, const char* message) {
	javaException(env, "java/lang/InterruptedException", message);
}

// from https://blogs.msdn.microsoft.com/twistylittlepassagesallalike/2011/04/23/everyone-quotes-command-line-arguments-the-wrong-way/
/*++
Routine Description:
    
    This routine appends the given argument to a command line such
    that CommandLineToArgvW will return the argument string unchanged.
    Arguments in a command line should be separated by spaces; this
    function does not add these spaces.
    
Arguments:
    
    Argument - Supplies the argument to encode.

    CommandLine - Supplies the command line to which we append the encoded argument string.

    Force - Supplies an indication of whether we should quote
            the argument even if it does not contain any characters that would
            ordinarily require quoting.
    
Return Value:
    
    None.
    
Environment:
    
    Arbitrary.
--*/
static void ArgvQuote (
    const std::wstring& Argument,
    std::wstring& CommandLine,
    bool Force
) {
    //
    // Unless we're told otherwise, don't quote unless we actually
    // need to do so --- hopefully avoid problems if programs won't
    // parse quotes properly
    //
    
    if (Force == false &&
        Argument.empty () == false &&
        Argument.find_first_of (L" \t\n\v\"") == Argument.npos) {
        CommandLine.append (Argument);
    } else {
        CommandLine.push_back (L'"');
        
        for (auto It = Argument.begin () ; ; ++It) {
            unsigned NumberBackslashes = 0;
        
            while (It != Argument.end () && *It == L'\\') {
                ++It;
                ++NumberBackslashes;
            }
        
            if (It == Argument.end ()) {
                
                //
                // Escape all backslashes, but let the terminating
                // double quotation mark we add below be interpreted
                // as a metacharacter.
                //
                
                CommandLine.append (NumberBackslashes * 2, L'\\');
                break;
            } else if (*It == L'"') {

                //
                // Escape all backslashes and the following
                // double quotation mark.
                //
                
                CommandLine.append (NumberBackslashes * 2 + 1, L'\\');
                CommandLine.push_back (*It);
            } else {
                
                //
                // Backslashes aren't special here.
                //
                
                CommandLine.append (NumberBackslashes, L'\\');
                CommandLine.push_back (*It);
            }
        }
    
        CommandLine.push_back (L'"');
    }
}

class NativeProcess {
private:
public:
	const PROCESS_INFORMATION procInfo;
	const STARTUPINFOW startupInfo;
	HANDLE stdOutPipeIn;
	HANDLE stdOutPipeOut;
	HANDLE interruptEvent;
	jint flags;
	
	NativeProcess(
			const PROCESS_INFORMATION& pi, 
			const STARTUPINFOW& si, 
			HANDLE stdoutpipein, 
			HANDLE stdoutpipeout,
			jint flags,
			HANDLE interruptevent)
		: 
			procInfo(pi), 
			startupInfo(si),
			stdOutPipeIn(stdoutpipein),
			stdOutPipeOut(stdoutpipeout),
			interruptEvent(interruptevent),
			flags(flags) {
	}
};
struct HandleCloser {
	HANDLE handle;
	
	HandleCloser(HANDLE handle = INVALID_HANDLE_VALUE) : handle(handle) {
	}
	
	~HandleCloser() {
		if(handle != INVALID_HANDLE_VALUE) {
			CloseHandle(handle);
		}
	}
};


#define PIPE_NAME_PREFIX "\\\\.\\pipe\\"

JNIEXPORT jlong JNICALL Java_saker_msvc_proc_NativeProcess_native_1startProcess(
	JNIEnv* env, 
	jclass clazz, 
	jstring exe, 
	jobjectArray commands, 
	jstring workingdirectory,
	jint flags, 
	jstring pipeid,
	jlong interrupteventptr
) {
	PROCESS_INFORMATION pi;
	ZeroMemory(&pi, sizeof(pi));
	
	STARTUPINFOW si;
	ZeroMemory(&si, sizeof(si));
	si.cb = sizeof(si);
	
	// To inherit the handles the bInheritHandle flag so pipe handles are inherited. 
	SECURITY_ATTRIBUTES secattry_inherit_handle = {};
	secattry_inherit_handle.nLength = sizeof(SECURITY_ATTRIBUTES);
	secattry_inherit_handle.bInheritHandle = TRUE;
	
	char pipename[sizeof(PIPE_NAME_PREFIX) + 3 + 64];

	memcpy(pipename, PIPE_NAME_PREFIX, sizeof(PIPE_NAME_PREFIX));
	jsize pipeidlen = env->GetStringLength(pipeid);
	if (pipeidlen >= 64) {
		failureType(env, "java/lang/IllegalArgumentException", "Invalid process pipe length", pipeidlen);
		return 0;
	}
	const jchar* pipeidchars = env->GetStringChars(pipeid, NULL);
	char* pipenameptr = pipename + (sizeof(PIPE_NAME_PREFIX) - 1);
	*pipenameptr++ = 's';
	*pipenameptr++ = 't';
	*pipenameptr++ = 'd';
	for(jsize i = 0; i < pipeidlen; ++i){
		*pipenameptr++ = (char) pipeidchars[i];
	}
	//terminating null
	*pipenameptr++ = 0;
	env->ReleaseStringChars(pipeid, pipeidchars);
	
	HANDLE namedpipe = CreateNamedPipe(
		pipename,
		PIPE_ACCESS_INBOUND | FILE_FLAG_FIRST_PIPE_INSTANCE | FILE_FLAG_OVERLAPPED,
		PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT | PIPE_REJECT_REMOTE_CLIENTS,
		1,
		16 * 1024,
		16 * 1024,
		INFINITE,
		NULL
	);
	if(namedpipe == INVALID_HANDLE_VALUE){
		failure(env, "CreateNamedPipe", GetLastError());
		return 0;
	}
	
	HANDLE writepipe = CreateFile(
		pipename,
		GENERIC_WRITE,
		0,
		&secattry_inherit_handle,
		OPEN_EXISTING,
		0,
		NULL
	);
	if(writepipe == INVALID_HANDLE_VALUE){
		failure(env, "CreateFile", GetLastError());
		return 0;
	}
	
	HANDLE stdoutpipein = namedpipe;
	HANDLE stdoutpipeout = writepipe;
	
	si.hStdError = stdoutpipeout;
	si.hStdOutput = stdoutpipeout;
	si.hStdInput = INVALID_HANDLE_VALUE;
	si.dwFlags |= STARTF_USESTDHANDLES;

	std::wstring modname = Java_To_WStr(env, exe);
	std::wstring workdir = Java_To_WStr(env, workingdirectory);
 	jsize cmdlen = env->GetArrayLength(commands);
 	std::wstring cmdstr;
 	
 	//include the executable as the zeroth argument
 	ArgvQuote(modname, cmdstr, false);
 	for(jsize i = 0; i < cmdlen; ++i){
 		jstring c = static_cast<jstring>(env->GetObjectArrayElement(commands, i));
 		std::wstring cstr = Java_To_WStr(env, c);
		cmdstr.push_back (L' ');
 		ArgvQuote(cstr, cmdstr, false);
 		env->DeleteLocalRef(c);
 	}
	
	wchar_t* cmd = new wchar_t[cmdstr.length() + 1];
	wcscpy(cmd, cmdstr.c_str());
	cmd[cmdstr.length()] = 0;
	
	//TODO make environment configureable


	//notes: the CREATE_NO_WINDOW flag increases startup time SIGNIFICANTLY. like + 15 ms or so for simple processes
	//       not specifying it creates a new console when used without one. e.g. in eclipse
	//       the DETACHED_PROCESS solves the console creation, and the startup time
	if (!CreateProcessW(
			modname.c_str(),	// exe path
			cmd,				// Command line
			NULL,       		// process security attributes
			NULL,       		// thread security attributes
			TRUE,      			// handle inheritance
			DETACHED_PROCESS,	// creation flags
			NULL,				// environment block
			workdir.c_str(),	// working directory 
			&si,        		// STARTUPINFO
			&pi)        		// PROCESS_INFORMATION
		) {
		failure(env, "CreateProcess", GetLastError());
		return 0;
	}
	
	//don't need the thread handle, close it right away
	CloseHandle(pi.hThread);
	
	NativeProcess* proc = new NativeProcess(pi, si, stdoutpipein, stdoutpipeout, flags, reinterpret_cast<HANDLE>(interrupteventptr));

	return reinterpret_cast<jlong>(proc);
}

JNIEXPORT void JNICALL Java_saker_msvc_proc_NativeProcess_native_1processIO(
	JNIEnv* env, 
	jclass clazz, 
	jlong nativeptr, 
	jobject processor, 
	jobject bytedirectbuffer,
	jobject errbytedirectbuffer
) {
	NativeProcess* proc = reinterpret_cast<NativeProcess*>(nativeptr);
	
	jmethodID stdinnotifymethod = env->GetStaticMethodID(
		clazz,
		"rewindNotifyStandardInput", 
		"(Ljava/nio/ByteBuffer;ILsaker/msvc/proc/NativeProcess$IOProcessor;)Z"
	);
	if(stdinnotifymethod == NULL){
		failureType(env, "java/lang/AssertionError", "GetMethodID", NULL);
		return;
	}
	
	void* bufaddress = env->GetDirectBufferAddress(bytedirectbuffer);
	if (bufaddress == NULL) {
		failureType(env, "java/lang/IllegalArgumentException", "GetDirectBufferAddress", NULL);
		return;
	}
	jlong capacity = env->GetDirectBufferCapacity(bytedirectbuffer);
	
	OVERLAPPED overlapped;
	ZeroMemory(&overlapped, sizeof(overlapped));
	overlapped.hEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
	if (overlapped.hEvent == NULL) {
		failure(env, "CreateEvent", GetLastError());
		return;
	}
	HandleCloser evh(overlapped.hEvent);
	
	HANDLE waits[] = { overlapped.hEvent, proc->procInfo.hProcess, proc->interruptEvent };
	
	bool interrupted = false;
	while (true) {
		BOOL success = ReadFile(proc->stdOutPipeIn, bufaddress, capacity, NULL, &overlapped);
		if (!success) {
			DWORD lasterror = GetLastError();
			if (lasterror != ERROR_IO_PENDING) {
				failure(env, "ReadFile", lasterror);
				return;
			}
		}
		
		DWORD waitres = WaitForMultipleObjects(3, waits, FALSE, INFINITE);
		switch(waitres) {
			case WAIT_OBJECT_0: {
				//read finished
				DWORD read;
				if(!GetOverlappedResult(proc->stdOutPipeIn, &overlapped, &read, FALSE)){
					failure(env, "GetOverlappedResult", GetLastError());
					return;
				}
				if(read > 0){
					jboolean continueprocessing = env->CallStaticBooleanMethod(clazz, stdinnotifymethod, bytedirectbuffer, read, processor);
					
					if (!continueprocessing) {
						return;
					}
					if (env->ExceptionCheck()) {
						return;
					}
				}
				if (interrupted) {
					//interrupted meanwhile, throw it, dont restart IO
					interruptException(env, "Process IO processing interrupted.");
					return;
				}
				//read is restarted next loop
				break;
			}
			case WAIT_OBJECT_0 + 1: {
				//process finished
				
				HandleCloser inh(proc->stdOutPipeIn);
				proc->stdOutPipeIn = INVALID_HANDLE_VALUE;
				
				CloseHandle(proc->stdOutPipeOut);
				proc->stdOutPipeOut = INVALID_HANDLE_VALUE;
				
				DWORD read = 0;
				if (!GetOverlappedResult(inh.handle, &overlapped, &read, TRUE)) {
					DWORD lasterror = GetLastError();
					if(lasterror != ERROR_BROKEN_PIPE){
						failure(env, "GetOverlappedResult", lasterror);
						return;
					}
				} else if(read > 0) {
					env->CallStaticBooleanMethod(clazz, stdinnotifymethod, bytedirectbuffer, read, processor);
					if (env->ExceptionCheck()) {
						return;
					}
					
					while (ReadFile(inh.handle, bufaddress, capacity, &read, NULL)) {
						if(read <= 0){
							break;
						}
						
						env->CallStaticBooleanMethod(clazz, stdinnotifymethod, bytedirectbuffer, read, processor);
						if (env->ExceptionCheck()) {
							return;
						}
					}
				}
				return;
			}
			case WAIT_OBJECT_0 + 2: {
				//interrupted
				{
					jclass threadc = env->FindClass("java/lang/Thread");
					jmethodID interruptedmethod = env->GetStaticMethodID(threadc, "interrupted", "()Z");
					if (!env->CallStaticBooleanMethod(threadc, interruptedmethod)) {
						//false alarm? continue the loop
						continue;
					}
					//no longer needed
					env->DeleteLocalRef(threadc);
				}
				//we've been interrupted. cancel any pending IO, check process exit, and if we're not finished,
				//throw the interrupted exception
				if (!CancelIoEx(proc->stdOutPipeIn, &overlapped)) {
					//failed to cancel the IO.
					//do not take the interrupt request into account
					//do not reinterrupt, as we would get into a loop
					//set internal interrupted flag so we can handle when the IO completes
					interrupted = true;
					continue;
				}
				//try getting the result without waiting
				DWORD read = 0;
				if (!GetOverlappedResult(proc->stdOutPipeIn, &overlapped, &read, TRUE)) {
					//failed to wait for the overlapped result
					DWORD lasterror = GetLastError();
					if(lasterror == ERROR_OPERATION_ABORTED){
						//cancelled
						//XXX can there be any bytes in the buffer that we need to process?
					} else {
						if(lasterror == ERROR_IO_PENDING) {
							//shouldn't really happen, check anyway
							//we can't throw the exception, continue the loop
							interrupted = true;
							continue;
						}
						//unrecognized error
						failure(env, "GetOverlappedResult", lasterror);
						return;
					}
				} else {
					if (read > 0) {
						//handle read bytes
						env->CallStaticBooleanMethod(clazz, stdinnotifymethod, bytedirectbuffer, read, processor);
						if (env->ExceptionCheck()) {
							return;
						}
					}
				}
				
				interruptException(env, "Process IO processing interrupted.");
				return;
			}
			default: {
				failure(env, "WaitForMultipleObjects", GetLastError());
				return;
			}
		}
	}
	
}

JNIEXPORT jint JNICALL Java_saker_msvc_proc_NativeProcess_native_1waitFor(
	JNIEnv* env, 
	jclass clazz, 
	jlong nativeptr, 
	jlong timeoutmillis
) {
	NativeProcess* proc = reinterpret_cast<NativeProcess*>(nativeptr);
	DWORD waitmillis = timeoutmillis == -1 ? INFINITE : timeoutmillis;
	
	DWORD ecode = -1;
	if (GetExitCodeProcess(proc->procInfo.hProcess, &ecode)) {
		if(ecode != STILL_ACTIVE) {
			//already finished
			return ecode;
		}
		//or it returned STILL_ACTIVE as result code 
		//wait for the process to complete
	}
	
	
	HANDLE waits[] = { proc->procInfo.hProcess, proc->interruptEvent };
	while(true) {
		DWORD waitres = WaitForMultipleObjects(2, waits, FALSE, INFINITE);
		switch(waitres) {
			case WAIT_OBJECT_0: {
				//process finished
				DWORD ecode = -1;
				if (GetExitCodeProcess(proc->procInfo.hProcess, &ecode)) {
					return ecode;
				}
				failure(env, "GetExitCodeProcess", GetLastError());
				return -1;
			}
			case WAIT_OBJECT_0 + 1: {
				//interrupted event signaled
				//check if we're really interrupted
				jclass threadc = env->FindClass("java/lang/Thread");
				jmethodID interruptedmethod = env->GetStaticMethodID(threadc, "interrupted", "()Z");
				if (env->CallStaticBooleanMethod(threadc, interruptedmethod)) {
					interruptException(env, "Process waiting interrupted.");
					return -1;
				}
				//false alarm? try waiting again
				env->DeleteLocalRef(threadc);
				continue;
			}
			default: {
				failure(env, "WaitForMultipleObjects", GetLastError());
				return -1;
			}
		}
	}
}
JNIEXPORT void JNICALL Java_saker_msvc_proc_NativeProcess_native_1interrupt(
	JNIEnv* env, 
	jclass clazz, 
	jlong interrupteventptr
) {
	HANDLE eventh = reinterpret_cast<HANDLE>(interrupteventptr);
	SetEvent(eventh);
}
JNIEXPORT jint JNICALL Java_saker_msvc_proc_NativeProcess_native_1getExitCode(
	JNIEnv* env, 
	jclass clazz, 
	jlong nativeptr
) {
	NativeProcess* proc = reinterpret_cast<NativeProcess*>(nativeptr);
	
	DWORD ecode = -1;
	if (GetExitCodeProcess(proc->procInfo.hProcess, &ecode)) {
		if (ecode == STILL_ACTIVE) {
			//process either exited with STILL_ACTIVE return code, or is still alive.
			//check if alive and return accordingly
			DWORD ret = WaitForSingleObject(proc->procInfo.hProcess, 0);
			if (ret == WAIT_TIMEOUT) {
				failureType(env, "java/lang/IllegalThreadStateException", "GetExitCodeProcess", STILL_ACTIVE);
				return -1;
			}
		    return STILL_ACTIVE;
		}
		return ecode;
	}
	failure(env, "GetExitCodeProcess", GetLastError());
	return -1;
}

JNIEXPORT void JNICALL Java_saker_msvc_proc_NativeProcess_native_1close(
	JNIEnv* env, 
	jclass clazz, 
	jlong nativeptr
) {
	NativeProcess* proc = reinterpret_cast<NativeProcess*>(nativeptr);
	
	CloseHandle(proc->procInfo.hProcess);
	if (proc->stdOutPipeIn != INVALID_HANDLE_VALUE){
		CloseHandle(proc->stdOutPipeIn);
	}
	if (proc->stdOutPipeOut != INVALID_HANDLE_VALUE){
		CloseHandle(proc->stdOutPipeOut);
	}
	
	delete proc;
}
JNIEXPORT jlong JNICALL Java_saker_msvc_proc_NativeProcess_native_1createInterruptEvent(
	JNIEnv* env, 
	jclass clazz
) {
	return reinterpret_cast<jlong>(CreateEvent(NULL, FALSE, FALSE, NULL));
}
JNIEXPORT void JNICALL Java_saker_msvc_proc_NativeProcess_native_1closeInterruptEvent(
	JNIEnv* env, 
	jclass clazz, 
	jlong interrupteventptr
) {
	HANDLE eventh = reinterpret_cast<HANDLE>(interrupteventptr);
	CloseHandle(eventh);
}