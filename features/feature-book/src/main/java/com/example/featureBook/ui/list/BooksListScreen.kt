package com.example.featureBook.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.ui.ObserveAsEvents
import com.example.featureBook.ui.UIStatefulContent
import com.example.featureBook.ui.UiState
import com.example.featureBook.ui.asString

@Composable
fun BooksListRoot(
    onNavigateToDetail: (String) -> Unit,
    viewModel: BooksListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is BooksListEvent.NavigateToDetail -> onNavigateToDetail(event.bookId)
        }
    }

    BooksListScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksListScreen(
    state: UiState<BooksListState>,
    onAction: (BooksListAction) -> Unit
) {
    val data = (state as? UiState.Success)?.data
    val localOnAction = remember { onAction }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (data?.isSearchActive == true) {
                        TextField(
                            value = data.searchQuery,
                            onValueChange = { localOnAction(BooksListAction.OnUpdateSearchQuery(it)) },
                            placeholder = { Text("Search by title or author") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Books")
                    }
                },
                actions = {
                    if (data?.isSearchActive == true) {
                        IconButton(onClick = { localOnAction(BooksListAction.OnSetSearchActive(false)) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { localOnAction(BooksListAction.OnSetSearchActive(true)) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { localOnAction(BooksListAction.OnToggleSortOrder) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = if (data?.sortOrder == SortOrder.ASCENDING) "Sort Z→A" else "Sort A→Z"
                            )
                        }
                        IconButton(onClick = { localOnAction(BooksListAction.OnToggleViewMode) }) {
                            Icon(
                                imageVector = if (data?.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = if (data?.viewMode == ViewMode.LIST) "Switch to grid" else "Switch to list"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            UIStatefulContent(
                state = state,
                loadingContent = {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                },
                errorContent = { uiText, _ ->
                    ErrorContent(
                        message = uiText.asString(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                },
                successContent = { booksListState ->
                    PullToRefreshBox(
                        isRefreshing = booksListState.isRefreshing,
                        onRefresh = { localOnAction(BooksListAction.OnRefresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (booksListState.viewMode == ViewMode.LIST) {
                            BooksListContent(
                                books = booksListState.displayedBooks,
                                initialScrollIndex = booksListState.savedScrollIndex,
                                onBookClick = { localOnAction(BooksListAction.OnBookClick(it)) },
                                onScrollPositionChange = { localOnAction(BooksListAction.OnSaveScrollPosition(it)) }
                            )
                        } else {
                            BooksGridContent(
                                books = booksListState.displayedBooks,
                                initialScrollIndex = booksListState.savedScrollIndex,
                                onBookClick = { localOnAction(BooksListAction.OnBookClick(it)) },
                                onScrollPositionChange = { localOnAction(BooksListAction.OnSaveScrollPosition(it)) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BooksListContent(
    books: List<BookUiModel>,
    initialScrollIndex: Int,
    onBookClick: (String) -> Unit,
    onScrollPositionChange: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onScrollPositionChange(it) }
    }

    LazyColumn(state = listState) {
        items(books, key = { it.id }) { book ->
            BookListItem(book = book, onClick = { onBookClick(book.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun BooksGridContent(
    books: List<BookUiModel>,
    initialScrollIndex: Int,
    onBookClick: (String) -> Unit,
    onScrollPositionChange: (Int) -> Unit
) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialScrollIndex)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }.collect { onScrollPositionChange(it) }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(books, key = { it.id }) { book ->
            BookGridItem(book = book, onClick = { onBookClick(book.id) })
        }
    }
}

@Composable
private fun BookListItem(book: BookUiModel, onClick: () -> Unit) {
    val fallbackPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = "Cover of ${book.title}",
            contentScale = ContentScale.Crop,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BookGridItem(book: BookUiModel, onClick: () -> Unit) {
    val fallbackPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please check your connection and try again",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
