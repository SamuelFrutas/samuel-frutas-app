package br.com.samuelfrutas.app

import android.widget.EditText

/** Compatibility property used by the programmatic native UI. */
private var EditText.singleLineCompat: Boolean
    get() = isSingleLine
    set(value) { setSingleLine(value) }

// Keeps the existing source syntax compatible with Kotlin versions where
// the Android singleLine synthetic property is not exposed.
var EditText.singleLine: Boolean
    get() = isSingleLine
    set(value) { setSingleLine(value) }
