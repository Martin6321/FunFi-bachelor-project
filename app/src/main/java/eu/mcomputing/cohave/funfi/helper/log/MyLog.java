package eu.mcomputing.cohave.funfi.helper.log;


import android.util.Log;

import eu.mcomputing.cohave.funfi.helper.config.Config;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class MyLog {
    public static void log(Class tag_class, String value) {
        MyLog.log(tag_class.getSimpleName(), value);
    }

    public static void log(String tag_class, String value) {
        //Log.d(Config.TAG, tag_class + "> " + value);
    }
}
