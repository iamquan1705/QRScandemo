import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.iamquan.qrcode.databinding.FragmentQrscodeBinding
import com.iamquan.qrcode.model.QR
import com.iamquan.qrcode.model.UrlQR
import com.iamquan.qrcode.model.WifiQR
import com.iamquan.qrcode.view.UrlView
import com.iamquan.qrcode.view.WifiView

class QRCodeFragment(private var qr: QR) : Fragment() {
    private lateinit var binding: FragmentQrscodeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQrscodeBinding.inflate(inflater, container, false)

        when (qr) {
            is WifiQR -> {
                Log.d("iamquan1705", "this is wifi qr")
                val wifiView = WifiView(requireContext())
                binding.llFragment.addView(wifiView)
            }
            is UrlQR -> {
                Log.d("iamquan1705", "this is url qr")
                val urlView = UrlView(requireContext())
                binding.llFragment.addView(urlView)
            }
        }
        return binding.root
    }
}