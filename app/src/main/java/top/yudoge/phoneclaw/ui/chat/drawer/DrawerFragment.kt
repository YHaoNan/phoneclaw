package top.yudoge.phoneclaw.ui.chat.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.databinding.FragmentDrawerBinding
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.ui.chat.ChatActivity

class DrawerFragment : Fragment() {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionAdapter: SessionAdapter

    private val sessionFacade by lazy { AppContainer.getInstance().sessionFacade }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupNewSessionButton()
        setupSettingsButton()
        loadSessions()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onSessionClick = { session ->
                (activity as? ChatActivity)?.loadSession(session.id)
                closeDrawer()
            },
            onSessionLongClick = { session ->
                showSessionContextMenu(session)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }
    }

    private fun setupNewSessionButton() {
        binding.newSessionButton.setOnClickListener {
            (activity as? ChatActivity)?.createNewSession()
            closeDrawer()
        }
    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            (activity as? ChatActivity)?.navigateToSettings()
            closeDrawer()
        }
    }

    fun loadSessions() {
        val sessions = sessionFacade.getAllSessions()
        sessionAdapter.submitList(sessions)
    }

    private fun closeDrawer() {
        (activity as? ChatActivity)?.closeDrawer()
    }

    private fun showSessionContextMenu(session: Session) {
        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(session.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(session)
                    1 -> showDeleteConfirmDialog(session)
                }
            }
            .show()
    }
    
    private fun showRenameDialog(session: Session) {
        val input = EditText(requireContext()).apply {
            setText(session.title)
            setSelection(text?.length ?: 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    sessionFacade.updateSessionTitle(session.id, newTitle)
                    loadSessions()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(session: Session) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("确定要删除这个对话吗？")
            .setPositiveButton(R.string.confirm) { _, _ ->
                sessionFacade.deleteSession(session.id)
                loadSessions()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadSessions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
