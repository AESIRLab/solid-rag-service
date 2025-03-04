package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aesirlab.model.Item
import org.aesirlab.usingcustomprocessorandroid.R

@Composable
fun MainScreen(
    items: List<Item>,
    onAddClick: (String) -> Unit,
    onIncreaseClick: (Item) -> Unit,
    onDecreaseClick: (Item) -> Unit,
    onDeleteClick: (Item) -> Unit
) {
    val appCtx = LocalContext.current.applicationContext
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
                    ItemComp(item, onIncreaseClick, onDecreaseClick, onDeleteClick)
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
        Text(text = item.amount.toString(), style = TextStyle(fontSize = 32.sp), modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp))

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