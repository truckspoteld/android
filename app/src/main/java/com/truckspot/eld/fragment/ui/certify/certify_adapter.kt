import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ArrayAdapter
import com.truckspot.eld.R
import com.truckspot.eld.models.CertifyModelItem

class CertifyAdapter(
    context: Context,
    val items: List<CertifyModelItem>,
    private val onCertifyClick: (String) -> Unit
) : ArrayAdapter<CertifyModelItem>(context, R.layout.certify_tile, items) {

    private val updatingItems = mutableSetOf<String>()

    fun setItemUpdating(date: String?, isUpdating: Boolean) {
        date?.let { safeDate ->
            if (isUpdating) {
                updatingItems.add(safeDate)
            } else {
                updatingItems.remove(safeDate)
            }
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.certify_tile, parent, false)

        val dateTextView = view.findViewById<TextView>(R.id.tvDate)
        val btnCertify = view.findViewById<TextView>(R.id.btnCertify)

        val item = items[position]
        dateTextView.text = item.date ?: "Unknown Date"

        val isUpdating = item.date?.let { updatingItems.contains(it) } ?: false
        btnCertify.isEnabled = !isUpdating
        btnCertify.text = if (isUpdating) "Updating..." else "Certify"

        btnCertify.setOnClickListener {
            if (!isUpdating) {
                // Subtle premium scale feedback
                it.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(120)
                    .withEndAction {
                        it.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start()
                        onCertifyClick(item.date!!)
                    }
                    .start()
            }
        }

        return view
    }
}