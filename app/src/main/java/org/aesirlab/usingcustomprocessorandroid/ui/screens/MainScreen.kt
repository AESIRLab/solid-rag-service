package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.aesirlab.model.Item

@Composable
fun MainScreen(
    items: List<Item>,
    onAddClick: (String) -> Unit,
    onIncreaseClick: (Item) -> Unit,
    onDecreaseClick: (Item) -> Unit
) {
    val appCtx = LocalContext.current.applicationContext
    val itemName = remember {
        mutableStateOf("")
    }
    Column {
        Row {
            TextField(value = itemName.value, onValueChange = { itemName.value = it }, label = { Text("Add new Item") })
            Spacer(modifier = Modifier.padding(horizontal = 16.dp))
            Button(onClick = {
                if (itemName.value.isEmpty()) {
                    Toast
                        .makeText(appCtx, "Item name must not be empty!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    onAddClick(itemName.value)
                }
            }) {
                Text("Add item!")
            }
        }

        if (items.isEmpty()) {
            Text("Nothing in your items list!")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items) { item ->
                    ItemComp(item, onIncreaseClick, onDecreaseClick)
                }
            }
        }
    }
}

@Composable
fun ItemComp(
    item: Item,
    onIncreaseClick: (Item) -> Unit,
    onDecreaseClick: (Item) -> Unit
) {
    Row {
        Text(text = item.name)
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Text(text = item.amount.toString())
        Button(onClick = { onIncreaseClick(item) }) {
            Text("^")
        }
        Button(onClick = { onDecreaseClick(item) }) {
            Text("V")
        }
    }
}