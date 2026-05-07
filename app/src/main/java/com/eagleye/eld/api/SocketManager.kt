import android.util.Log
import com.eagleye.eld.utils.Constants
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private val SOCKET_URL: String get() = Constants.SOCKET_URL
    private var socket: Socket? = null
    @Volatile
    private var isConnecting = false

    fun initialize() {
        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000  // Reduced timeout to 20 seconds to fail faster on resume
                transports = arrayOf("polling", "websocket")  // Allow both polling and websocket
            }
            socket = IO.socket(SOCKET_URL,options)
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected to server!")
                isConnecting = false
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "Connection Error: ${args.getOrNull(0)}")
                isConnecting = false
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Disconnected from server")
                isConnecting = false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            isConnecting = false
        }
    }

    fun connect(id : Int) {
        // Run connection on background thread to avoid blocking main thread
        Thread {
            try {
                // Prevent concurrent connection attempts
                if (isConnecting) {
                    Log.d("SocketIO", "Already connecting, skipping duplicate connect")
                    return@Thread
                }
                
                // If socket is already connected, just send driver ID
                if (socket?.connected() == true) {
                    Log.d("SocketIO", "Socket already connected, sending driver ID")
                    sendMessage("sendDriverId", id)
                    return@Thread
                }
                
                isConnecting = true
                Log.d("SocketIO", "Starting socket connection...")
                
                // Disconnect first if socket exists but isn't connected (clean state)
                try {
                    socket?.disconnect()
                } catch (e: Exception) {
                    Log.w("SocketIO", "Error during disconnect cleanup: ${e.message}")
                }
                
                socket?.connect()
                sendMessage("sendDriverId", id)
            } catch (e: Exception) {
                Log.e("SocketIO", "Error connecting socket: ${e.message}")
                isConnecting = false
            }
        }.start()
    }

    fun disconnect() {
        try {
            socket?.off("newLogs")
            socket?.off("logUpdated")
            socket?.disconnect()
            isConnecting = false
            Log.d("SocketIO", "Socket disconnected and listeners cleared")
        } catch (e: Exception) {
            Log.e("SocketIO", "Error during disconnect: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    fun listenForLogs(onLogsReceived: (JSONObject) -> Unit) {
        socket?.off("newLogs")
        socket?.on("newLogs") { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                onLogsReceived(args[0] as JSONObject)
            }
        }
        Log.d("SocketIO", "Listening for newLogs events")
    }

    /** Call when server emits logUpdated (after add/update/delete log). Client should refetch home/logs. */
    fun listenForLogUpdates(onLogUpdated: () -> Unit) {
        socket?.off("logUpdated")
        socket?.on("logUpdated") {
            Log.d("SocketIO", "logUpdated received, triggering refresh")
            onLogUpdated()
        }
        Log.d("SocketIO", "Listening for logUpdated events")
    }

    fun listenForCodriverRequest(onRequest: (fromDriverId: Int, fromDriverName: String, fromUsername: String, companyName: String) -> Unit) {
        socket?.off("codriverRequest")
        socket?.on("codriverRequest") { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val json = args[0] as JSONObject
                val fromDriverId = json.optInt("fromDriverId", 0)
                val fromDriverName = json.optString("fromDriverName", "A driver")
                val fromUsername = json.optString("fromUsername", "")
                val companyName = json.optString("companyName", "")
                if (fromDriverId > 0) {
                    onRequest(fromDriverId, fromDriverName, fromUsername, companyName)
                }
            }
        }
        Log.d("SocketIO", "Listening for codriverRequest events")
    }

    fun stopListeningForCodriverRequest() {
        socket?.off("codriverRequest")
    }

    fun sendMessage(event: String, data: Int) {
        socket?.emit(event, data)
    }
}

