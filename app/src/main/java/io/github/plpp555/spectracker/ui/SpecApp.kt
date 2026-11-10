package io.github.plpp555.spectracker.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.plpp555.spectracker.collector.SpecCatalog
import io.github.plpp555.spectracker.model.SpecCategory
import io.github.plpp555.spectracker.model.SpecGroup
import io.github.plpp555.spectracker.model.toPlainText
import io.github.plpp555.spectracker.util.EditionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecApp(edition: EditionInfo = EditionInfo.NONE) {
    val context = LocalContext.current
    val ultra = edition.isUltra
    val categories = SpecCatalog.categories
    var selected by rememberSaveable { mutableIntStateOf(0) }
    // タブを切り替えても、取得済みの内容を一瞬で表示できるようにキャッシュする
    val cache = remember { mutableStateMapOf<String, List<SpecGroup>>() }
    val scope = rememberCoroutineScope()

    // 特装版（Edition）: 起動直後の 1 秒間だけ画面を暗くして端末名を表示する。画面回転では再表示しない。
    var splashVisible by rememberSaveable(edition.editionName) { mutableStateOf(edition.editionName != null) }
    LaunchedEffect(edition.editionName) {
        if (splashVisible) {
            delay(1000)
            splashVisible = false
        }
    }

    // Ultra は背景が暗い銀河のため、システムバーのアイコンを明色にする
    LaunchedEffect(ultra) {
        if (ultra) {
            (context as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    if (ultra) GalaxyBackground()
    Scaffold(
        containerColor = if (ultra) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (ultra) Color.Transparent else MaterialTheme.colorScheme.surface,
                    ),
                    title = {
                        Column {
                            if (ultra) {
                                Text(
                                    text = "Ultra Ver",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD1C4FF),
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("Spec-tracker", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "  試作版 V1",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { shareAll(context) } }) {
                            Icon(Icons.Default.Share, contentDescription = "すべてのスペックを共有")
                        }
                    },
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = selected,
                    edgePadding = 8.dp,
                    containerColor = if (ultra) Color.Transparent else MaterialTheme.colorScheme.surface,
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == selected,
                            onClick = { selected = index },
                            text = { Text(category.title) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        CategoryContent(
            category = categories[selected],
            cache = cache,
            translucent = ultra,
            modifier = Modifier.padding(padding),
        )
    }
    edition.editionName?.let { EditionSplash(name = it, visible = splashVisible) }
    }
}

@Composable
private fun CategoryContent(
    category: SpecCategory,
    cache: MutableMap<String, List<SpecGroup>>,
    translucent: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    // 画面が見えている間だけ取得する。ライブ更新のカテゴリは一定間隔で再取得する。
    LaunchedEffect(category.id) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                cache[category.id] = withContext(Dispatchers.Default) { category.collect(context) }
                if (category.refreshMillis <= 0) break
                delay(category.refreshMillis)
            }
        }
    }

    val groups = cache[category.id]
    Box(modifier = modifier.fillMaxSize()) {
        if (groups == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups) { group -> GroupCard(group, translucent) }
            }
        }
    }
}

@Composable
private fun GroupCard(group: SpecGroup, translucent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // 銀河の背景が透けて見えるよう、Ultra ではカードを半透明にする
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (translucent) 0.72f else 1f),
        ),
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                group.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                group.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                }
            }
        }
    }
}

/** すべてのカテゴリを取得し、共有シート（テキスト）を開く。外部への送信はユーザーが共有先を選んだときだけ。 */
private suspend fun shareAll(context: Context) {
    val text = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        @Suppress("DEPRECATION")
        val version = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        SpecCatalog.categories
            .map { it.title to it.collect(appContext) }
            .toPlainText("Spec-tracker $version")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Spec-tracker スペック情報")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "スペック情報を共有"))
}
