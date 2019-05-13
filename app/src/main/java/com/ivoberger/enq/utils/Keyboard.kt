/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.systemservices.inputMethodManager

fun View.hideKeyboard() {
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
}

fun AppCompatActivity.hideKeyboard() = lifecycleScope.launch {
    // Find the currently focused view, so we can grab the correct window token from it.
    val view = currentFocus ?: findViewById<View>(android.R.id.content).rootView
    view.hideKeyboard()
}

fun Fragment.hideKeyboard() = view?.rootView?.hideKeyboard()