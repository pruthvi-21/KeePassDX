package com.kunzisoft.keepass.password

import me.gosimple.nbvcxz.resources.Generator

class PassphraseGenerator {

    @Throws(IllegalArgumentException::class)
    fun generatePassphrase(wordCount: Int,
                           wordSeparator: String,
                           wordCase: WordCase): String {
        // From eff_large dictionary
        return when (wordCase) {
            WordCase.LOWER_CASE -> {
                Generator.generatePassphrase(wordSeparator, wordCount)
            }
            WordCase.UPPER_CASE -> {
                applyWordCase(wordCount, wordSeparator) { word ->
                    word.uppercase()
                }
            }
            WordCase.TITLE_CASE -> {
                applyWordCase(wordCount, wordSeparator) { word ->
                    word.replaceFirstChar { char -> char.uppercaseChar() }
                }
            }
        }
    }

    private fun applyWordCase(wordCount: Int,
                              wordSeparator: String,
                              wordAction: (word: String) -> String): String {
        val splitWords = Generator.generatePassphrase(TEMP_SPLIT, wordCount).split(TEMP_SPLIT)
        val stringBuilder = StringBuilder()
        splitWords.forEach {
            stringBuilder
                .append(wordAction(it))
                .append(wordSeparator)
        }
        return stringBuilder.toString().removeSuffix(wordSeparator)
    }

    enum class WordCase {
        LOWER_CASE,
        UPPER_CASE,
        TITLE_CASE;

        companion object {
            fun getByOrdinal(position: Int): WordCase {
                return when (position) {
                    0 -> LOWER_CASE
                    1 -> UPPER_CASE
                    else -> TITLE_CASE
                }
            }
        }
    }

    companion object {
        private const val TEMP_SPLIT = "-"
    }
}
