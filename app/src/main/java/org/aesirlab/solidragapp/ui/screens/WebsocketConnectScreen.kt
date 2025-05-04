package org.aesirlab.solidragapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
import org.aesirlab.mylibrary.websockets.generateDPoPKeyPair
import org.aesirlab.mylibrary.websockets.inrupt.StaticClientCredentials
import org.aesirlab.mylibrary.websockets.inrupt.createNotificationConnectionRequest
import org.aesirlab.mylibrary.websockets.inrupt.createProtocolNegotiationRequest
import org.aesirlab.mylibrary.websockets.inrupt.createSocketRequest
import org.aesirlab.mylibrary.websockets.inrupt.getAccessToken
import org.aesirlab.mylibrary.websockets.inrupt.makeStaticInruptTokenRequest
import org.aesirlab.mylibrary.websockets.inrupt.wellKnownSolidRequest

@Composable
fun WebsocketConnectScreen() {
    val clientId = ""
    val clientSecret = ""
    val exampleEndpoint = "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/AndroidApplicationTest/"
    val credentials = StaticClientCredentials(clientId, clientSecret)
    val dpopKey = generateDPoPKeyPair()
    val tokenRequest = makeStaticInruptTokenRequest(credentials, dpopKey)
    val client = createUnsafeOkHttpClient()
    val coroutineScope = rememberCoroutineScope()
    val wellKnownSolidRequest = wellKnownSolidRequest(exampleEndpoint)
    LaunchedEffect(key1 = Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val tokenResponse = client.newCall(tokenRequest).execute()
                val accessToken = getAccessToken(tokenResponse)
                // dont need two  lines above two if access token is available
                val wellKnownSolidResponse = client.newCall(wellKnownSolidRequest).execute()
                val protocolNegotiationRequest = createProtocolNegotiationRequest(wellKnownSolidResponse, dpopKey, accessToken)
                val protocolNegotiationResponse = client.newCall(protocolNegotiationRequest).execute()
                val notificationConnectionRequest = createNotificationConnectionRequest(protocolNegotiationResponse, dpopKey, accessToken, exampleEndpoint)
                val notificationConnectionResponse = client.newCall(notificationConnectionRequest).execute()
                val socketRequest = createSocketRequest(notificationConnectionResponse)
            }
        }
    }


}