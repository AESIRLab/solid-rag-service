package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.aesirlab.model.Item
import org.aesirlab.usingcustomprocessorandroid.R
import org.aesirlab.usingcustomprocessorandroid.ui.ItemViewModel
import org.aesirlab.usingcustomprocessorandroid.ui.SolidMobileItemApplication
import org.aesirlab.usingcustomprocessorandroid.model.AuthTokenStore


@Composable
fun MainScreen(
    onLogoutClick: () -> Unit
) {
    val appCtx = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val store = AuthTokenStore(appCtx)
    val viewModel: ItemViewModel = viewModel(
        factory = ItemViewModel.Factory
    )

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        runBlocking {
            val webId = store.getWebId().first()
            viewModel.updateWebId(webId)
        }
    }


    val items by viewModel.allItems.collectAsState()

    fun onAddClick(thing: String) {
        val newItem = Item("", thing)
        coroutineScope.launch {
            viewModel.insert(newItem)
        }
    }

    fun onIncreaseClick(item: Item) {
        item.amount += 1
        coroutineScope.launch {
            viewModel.update(item)
        }
    }

    fun onDecreaseClick(item: Item) {
        if (item.amount > 0) {
            item.amount -= 1
            coroutineScope.launch {
                viewModel.update(item)
            }
        }
    }

    fun onDeleteClick(item: Item) {
        coroutineScope.launch {
            viewModel.delete(item)
        }
    }

    val itemName = remember {
        mutableStateOf("")
    }
    Column {
        Row {
            TextField(value = itemName.value, onValueChange = { itemName.value = it }, label = { Text("Add new Item") })
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Button(onClick = {
                if (itemName.value.isEmpty()) {
                    Toast
                        .makeText(appCtx, "Item name must not be empty!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    onAddClick(itemName.value)
                    itemName.value = ""
                }
            }) {
                Text("Add item!")
            }
        }
        Button(onClick = onLogoutClick ) {
            Text("Logout")
        }

        if (items.isEmpty()) {
            Text("Nothing in your items list!")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items) { item ->
                    ItemComp(item,
                        onIncreaseClick = { selectedItem ->
                            onIncreaseClick(selectedItem)
                        }, onDecreaseClick = { selectedItem ->
                            onDecreaseClick(selectedItem)
                        }, onDeleteClick = { selectedItem ->
                            onDeleteClick(selectedItem)
                        })
                }
            }
        }
    }
}

@Composable
fun ItemComp(
    item: Item,
    onIncreaseClick: (Item) -> Unit,
    onDecreaseClick: (Item) -> Unit,
    onDeleteClick: (Item) -> Unit
) {
    val name = item.name.split("^^")[0]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, style = TextStyle(fontSize = 32.sp), modifier = Modifier.fillMaxHeight())
        Spacer(modifier = Modifier.weight(1f))
        Text(text = item.amount.toString(), style = TextStyle(fontSize = 32.sp), modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp))

        Button(onClick = { onIncreaseClick(item) }) {
            Image(
                painter = painterResource(id = R.drawable.up_arrow),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        Button(onClick = { onDecreaseClick(item) }) {
            Image(
                painter = painterResource(id = R.drawable.down_arrow),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
                )
        }
        Button(onClick = { onDeleteClick(item) }) {
            Text("Delete Item")
        }


    }
}