import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

//    private const val SOCKET_URL = "https://20hx23jc-4501.asse.devtunnels.ms/"
    private const val SOCKET_URL = "https://api.truckspoteld.com"
    private var socket: Socket? = null

    fun initialize() {
        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 60000  // Increase timeout to 10 second // s
                transports = arrayOf("websocket")  // Force WebSocket
            }
            socket = IO.socket(SOCKET_URL,options)
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected to server!")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "Connection Error: ${args[0]}")
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Disconnected from server")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect(id : Int) {
        socket?.connect()
        sendMessage("sendDriverId" ,id)
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    fun listenForLogs(onLogsReceived: (JSONObject) -> Unit) {
        isConnected()
        socket?.on("newLogs") { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                onLogsReceived(args[0] as JSONObject)
            }
        }
    }

    fun sendMessage(event: String, data: Int) {
        socket?.emit(event, data)
    }
}

