package tech.wideas.clad.ui.import

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main import screen that coordinates the import flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = koinViewModel(),
    onDismiss: () -> Unit,
    onImportComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.flowState) {
                            is ImportFlowState.SelectMethod -> "Import Account"
                            is ImportFlowState.EnteringSeedPhrase -> "Seed Phrase"
                            is ImportFlowState.ScanningQrCode -> "Scan QR Code"
                            is ImportFlowState.EnteringAddress -> "Enter Address"
                            is ImportFlowState.Validating -> "Validating..."
                            is ImportFlowState.Confirming -> "Confirm Import"
                            is ImportFlowState.Saving -> "Saving..."
                            is ImportFlowState.Success -> "Success"
                            is ImportFlowState.Error -> "Error"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (uiState.flowState !is ImportFlowState.Success) {
                        IconButton(
                            onClick = {
                                when (uiState.flowState) {
                                    is ImportFlowState.SelectMethod -> onDismiss()
                                    else -> viewModel.goBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.flowState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            },
            label = "import_flow_animation"
        ) { flowState ->
            when (flowState) {
                is ImportFlowState.SelectMethod -> {
                    ImportMethodSelection(
                        onMethodSelected = { method ->
                            viewModel.selectMethod(method)
                        }
                    )
                }

                is ImportFlowState.EnteringSeedPhrase -> {
                    SeedPhraseInputScreen(
                        words = uiState.seedPhraseWords,
                        wordCount = uiState.wordCount,
                        error = uiState.mnemonicError,
                        onWordChanged = { index, word ->
                            viewModel.updateWord(index, word)
                        },
                        onWordCountChanged = { count ->
                            viewModel.setWordCount(count)
                        },
                        onValidate = {
                            viewModel.validateAndDeriveSeedPhrase()
                        },
                        canProceed = uiState.canProceed
                    )
                }

                is ImportFlowState.ScanningQrCode -> {
                    QrCodeScannerScreen(
                        error = uiState.qrCodeError,
                        onQrCodeScanned = { content ->
                            viewModel.onQrCodeScanned(content)
                        },
                        onManualEntry = {
                            viewModel.selectMethod(ImportMethod.MANUAL_ADDRESS)
                        }
                    )
                }

                is ImportFlowState.EnteringAddress -> {
                    ManualAddressScreen(
                        address = uiState.manualAddress,
                        error = uiState.addressError,
                        onAddressChanged = { address ->
                            viewModel.updateManualAddress(address)
                        },
                        onValidate = {
                            viewModel.validateManualAddress()
                        },
                        canProceed = uiState.canProceed
                    )
                }

                is ImportFlowState.Validating -> {
                    LoadingScreen(message = "Validating...")
                }

                is ImportFlowState.Confirming -> {
                    ConfirmImportScreen(
                        address = flowState.address,
                        isWatchOnly = flowState.keypair == null,
                        label = uiState.accountLabel,
                        keyType = uiState.keyType,
                        onLabelChanged = { label ->
                            viewModel.updateAccountLabel(label)
                        },
                        onKeyTypeChanged = { keyType ->
                            viewModel.updateKeyType(keyType)
                        },
                        onConfirm = {
                            scope.launch {
                                val result = viewModel.saveAccount()
                                if (result is ImportFlowState.Success) {
                                    onImportComplete()
                                }
                            }
                        },
                        canConfirm = uiState.canProceed,
                        error = uiState.error
                    )
                }

                is ImportFlowState.Saving -> {
                    LoadingScreen(message = "Securing your account...")
                }

                is ImportFlowState.Success -> {
                    ImportSuccessScreen(
                        account = flowState.account,
                        onDone = {
                            viewModel.reset()
                            onImportComplete()
                        }
                    )
                }

                is ImportFlowState.Error -> {
                    ImportErrorScreen(
                        message = flowState.message,
                        onRetry = {
                            viewModel.goBack()
                        },
                        onCancel = {
                            viewModel.reset()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
