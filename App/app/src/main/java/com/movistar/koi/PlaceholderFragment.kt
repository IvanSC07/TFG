package com.movistar.koi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.movistar.koi.databinding.FragmentPlaceholderBinding

/**
 * Fragmento para mostrar un mensaje de sección
 */
class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    /**
     * Crea la vista
     */
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        /**
         * Crea una nueva instancia del fragmento con los argumentos necesarios
         */
        fun newInstance(title: String, message: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Crea la vista
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Crea la vista
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: "Sección"
        val message = arguments?.getString(ARG_MESSAGE) ?: "Próximamente"

        binding.sectionTitle.text = title
        binding.sectionMessage.text = message
    }

    /**
     * Actualiza la vista
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}