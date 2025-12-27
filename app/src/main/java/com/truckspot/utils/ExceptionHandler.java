package com.truckspot.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;


public class ExceptionHandler implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler defaultHandler;
	private final Context context;

	ExceptionHandler(final Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
	public void uncaughtException(@NonNull Thread thread, final Throwable throwable) {
		final Writer stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		final String stacktrace = stringWriter.toString();
		printWriter.close();
		ExceptionHelper.writeToStacktraceFile(context, stacktrace);
		this.defaultHandler.uncaughtException(thread, throwable);
    }

}
