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
import top.yudoge.phoneclaw.databinding.FragmentDrawerBinding
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.ui.chat.ChatActivity
import java.util.Calendar

class DrawerFragment : Fragment() {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: PhoneClawDbHelper
    private lateinit var sessionAdapter: SessionGroupAdapter

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
        dbHelper = PhoneClawDbHelper(requireContext())
        
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
        sessionAdapter = SessionGroupAdapter(
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
        val sessions = dbHelper.allSessions
        val groupedSessions = groupSessionsByTime(sessions)
        sessionAdapter.submitList(groupedSessions)
    }

    private fun groupSessionsByTime(
        sessions: List<PhoneClawDbHelper.SessionRecord>
    ): List<SessionGroup> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val oneWeekAgo = calendar.timeInMillis

        val recentSessions = sessions.filter { it.updatedAt >= oneWeekAgo }
        val olderSessions = sessions.filter { it.updatedAt < oneWeekAgo }

        val groups = mutableListOf<SessionGroup>()

        if (recentSessions.isNotEmpty()) {
            groups.add(SessionGroup(getString(top.yudoge.phoneclaw.R.string.recent_week), recentSessions))
        }

        if (olderSessions.isNotEmpty()) {
            groups.add(SessionGroup(getString(top.yudoge.phoneclaw.R.string.earlier), olderSessions))
        }

        return groups
    }

    private fun closeDrawer() {
        (activity as? ChatActivity)?.closeDrawer()
    }

    private fun showSessionContextMenu(session: PhoneClawDbHelper.SessionRecord) {
        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(session.title ?: "新对话")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(session)
                    1 -> showDeleteConfirmDialog(session)
                }
            }
            .show()
    }
    
    private fun showRenameDialog(session: PhoneClawDbHelper.SessionRecord) {
        val input = EditText(requireContext()).apply {
            setText(session.title ?: "")
            setSelection(text?.length ?: 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    dbHelper.updateSessionTitle(session.id, newTitle)
                    loadSessions()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(session: PhoneClawDbHelper.SessionRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("确定要删除这个对话吗？")
            .setPositiveButton(R.string.confirm) { _, _ ->
                dbHelper.deleteSession(session.id)
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

data class SessionGroup(
    val title: String,
    val sessions: List<PhoneClawDbHelper.SessionRecord>
)
