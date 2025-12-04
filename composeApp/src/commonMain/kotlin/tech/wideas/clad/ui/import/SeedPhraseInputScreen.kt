package tech.wideas.clad.ui.import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen for entering seed phrase words.
 */
@Composable
fun SeedPhraseInputScreen(
    words: List<String>,
    wordCount: Int,
    error: String?,
    onWordChanged: (Int, String) -> Unit,
    onWordCountChanged: (Int) -> Unit,
    onPhrasePasted: (List<String>) -> Unit,
    onValidate: () -> Unit,
    canProceed: Boolean
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Word count selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Word count:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            FilterChip(
                selected = wordCount == 12,
                onClick = { onWordCountChanged(12) },
                label = { Text("12 words") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                selected = wordCount == 24,
                onClick = { onWordCountChanged(24) },
                label = { Text("24 words") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Word input grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(words.take(wordCount)) { index, word ->
                WordInputField(
                    index = index + 1,
                    word = word,
                    onWordChanged = { newWord -> onWordChanged(index, newWord) },
                    onPhrasePasted = onPhrasePasted,
                    onNext = {
                        if (index < wordCount - 1) {
                            focusManager.moveFocus(FocusDirection.Next)
                        } else {
                            focusManager.clearFocus()
                        }
                    },
                    isLast = index == wordCount - 1
                )
            }
        }

        // Error message
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import button
        Button(
            onClick = onValidate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            enabled = canProceed
        ) {
            Text("Import Account")
        }
    }
}

@Composable
private fun WordInputField(
    index: Int,
    word: String,
    onWordChanged: (String) -> Unit,
    onPhrasePasted: (List<String>) -> Unit,
    onNext: () -> Unit,
    isLast: Boolean
) {
    OutlinedTextField(
        value = word,
        onValueChange = { newValue ->
            // Check if user pasted a full phrase (multiple words separated by whitespace)
            val pastedWords = newValue
                .lowercase()
                .trim()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .map { it.filter { char -> char.isLetter() } }
                .filter { it.isNotBlank() }

            when {
                pastedWords.size == 12 || pastedWords.size == 24 -> {
                    // Full phrase pasted - auto-fill all fields
                    onPhrasePasted(pastedWords)
                }
                pastedWords.size > 1 -> {
                    // Partial paste - just use the first word
                    onWordChanged(pastedWords.first())
                }
                else -> {
                    // Single word or normal typing - filter to letters only
                    val filtered = newValue.filter { it.isLetter() }.lowercase()
                    onWordChanged(filtered)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = "$index",
                fontSize = 12.sp
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onNext() }
        ),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    )
}
