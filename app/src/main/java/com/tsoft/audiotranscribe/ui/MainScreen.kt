package com.tsoft.audiotranscribe.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tsoft.audiotranscribe.data.TranscriptResponse
import com.tsoft.audiotranscribe.data.TranscriptionState
import com.tsoft.audiotranscribe.data.Utterance
import com.tsoft.audiotranscribe.player.ClipPlayer

@Composable
fun MainScreen(viewModel: TranscribeViewModel, clipPlayer: ClipPlayer) {
    val state by viewModel.state.collectAsState()

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAudioUri(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transcripción de audio con identificación de hablantes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::setApiKey,
            label = { Text("API key de AssemblyAI") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }) {
                Text("Elegir audio")
            }
            Button(
                onClick = viewModel::startTranscription,
                enabled = state.audioUri != null
            ) {
                Text("Transcribir")
            }
        }

        state.audioUri?.let {
            Spacer(Modifier.height(8.dp))
            Text("Archivo seleccionado: ${it.lastPathSegment}")
        }

        Spacer(Modifier.height(16.dp))

        when (val ts = state.transcriptionState) {
            is TranscriptionState.Idle -> {}
            is TranscriptionState.Uploading -> ProgressRow(ts.progress)
            is TranscriptionState.Processing -> ProgressRow("Transcribiendo y detectando hablantes...")
            is TranscriptionState.Failed -> Text(
                "Error: ${ts.message}",
                color = MaterialTheme.colorScheme.error
            )
            is TranscriptionState.Done -> TranscriptView(
                result = ts.result,
                audioUri = state.audioUri,
                speakerNames = state.speakerNames,
                clipPlayer = clipPlayer,
                onRename = { speaker, newName -> viewModel.renameSpeaker(ts.result.id, speaker, newName) }
            )
        }
    }
}

@Composable
private fun ProgressRow(label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.height(20.dp))
        Spacer(Modifier.height(8.dp))
        Text("  $label")
    }
}

@Composable
private fun TranscriptView(
    result: TranscriptResponse,
    audioUri: Uri?,
    speakerNames: Map<String, String>,
    clipPlayer: ClipPlayer,
    onRename: (String, String) -> Unit
) {
    val utterances = result.utterances.orEmpty()
    var renameDialogSpeaker by remember { mutableStateOf<String?>(null) }

    if (utterances.isEmpty()) {
        Text("No se detectaron segmentos de habla.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(utterances) { utterance ->
            UtteranceCard(
                utterance = utterance,
                displayName = speakerNames[utterance.speaker] ?: "Hablante ${utterance.speaker}",
                onPlayClip = {
                    audioUri?.let { clipPlayer.playClip(it, utterance.start, utterance.end) }
                },
                onRenameClick = { renameDialogSpeaker = utterance.speaker }
            )
        }
    }

    renameDialogSpeaker?.let { speaker ->
        RenameDialog(
            currentName = speakerNames[speaker] ?: "Hablante $speaker",
            onDismiss = { renameDialogSpeaker = null },
            onConfirm = { newName ->
                onRename(speaker, newName)
                renameDialogSpeaker = null
            }
        )
    }
}

@Composable
private fun UtteranceCard(
    utterance: Utterance,
    displayName: String,
    onPlayClip: () -> Unit,
    onRenameClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { onRenameClick() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("▶ escuchar", modifier = Modifier.clickable { onPlayClip() })
                Text("✎ renombrar", modifier = Modifier.clickable { onRenameClick() })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(utterance.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar nombre al hablante") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Guardar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
