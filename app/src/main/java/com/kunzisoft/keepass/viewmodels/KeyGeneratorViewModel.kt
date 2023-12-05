package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class KeyGeneratorViewModel : ViewModel() {

    val keyGenerated: LiveData<String> get() = _keyGenerated
    private val _keyGenerated = MutableLiveData<String>()

    val keyGeneratedValidated: LiveData<Void?> get() = _keyGeneratedValidated
    private val _keyGeneratedValidated = SingleLiveEvent<Void?>()
    val requireKeyGeneration: LiveData<Void?> get() = _requireKeyGeneration
    private val _requireKeyGeneration = SingleLiveEvent<Void?>()

    val passwordGeneratedValidated: LiveData<Void?> get() = _passwordGeneratedValidated
    private val _passwordGeneratedValidated = SingleLiveEvent<Void?>()
    val requirePasswordGeneration: LiveData<Void?> get() = _requirePasswordGeneration
    private val _requirePasswordGeneration = SingleLiveEvent<Void?>()

    val passphraseGeneratedValidated: LiveData<Void?> get() = _passphraseGeneratedValidated
    private val _passphraseGeneratedValidated = SingleLiveEvent<Void?>()
    val requirePassphraseGeneration: LiveData<Void?> get() = _requirePassphraseGeneration
    private val _requirePassphraseGeneration = SingleLiveEvent<Void?>()

    fun setKeyGenerated(passKey: String) {
        _keyGenerated.value = passKey
    }

    fun validateKeyGenerated() {
        _keyGeneratedValidated.call()
    }

    fun validatePasswordGenerated() {
        _passwordGeneratedValidated.call()
    }

    fun validatePassphraseGenerated() {
        _passphraseGeneratedValidated.call()
    }

    fun requireKeyGeneration() {
        _requireKeyGeneration.call()
    }

    fun requirePasswordGeneration() {
        _requirePasswordGeneration.call()
    }

    fun requirePassphraseGeneration() {
        _requirePassphraseGeneration.call()
    }
}