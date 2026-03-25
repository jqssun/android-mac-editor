package io.github.jqssun.maceditor.utils

import android.text.Editable
import android.text.TextWatcher

class MacTextWatcher : TextWatcher {
    private var editing = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (editing || s == null) return
        editing = true

        val raw = s.toString().replace(":", "").uppercase()
        val sb = StringBuilder()
        for (i in raw.indices) {
            if (i > 0 && i % 2 == 0) sb.append(':')
            sb.append(raw[i])
        }
        val formatted = sb.toString()
        if (formatted != s.toString()) {
            s.replace(0, s.length, formatted)
        }

        editing = false
    }
}
