package com.stealthvault.app.ui.vault.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stealthvault.app.R
import com.stealthvault.app.data.local.entities.VaultNote
import com.stealthvault.app.databinding.FragmentNotesBinding
import com.stealthvault.app.ui.vault.VaultViewModel
import com.stealthvault.app.ui.vault.adapters.VaultNoteAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment(R.layout.fragment_notes) {

    private val viewModel: VaultViewModel by activityViewModels()
    private var fullNoteList: List<VaultNote> = emptyList()
    private var noteAdapter: VaultNoteAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNotesBinding.bind(view)

        noteAdapter = VaultNoteAdapter(
            onItemClick = { /* future: open note for editing */ },
            onItemLongClick = { note -> confirmDeleteNote(note) }
        )

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }

        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(R.id.noteEditFragment)
        }

        // Search filter
        binding.etSearchNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                filterNotes(s?.toString().orEmpty())
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collectLatest { notes ->
                fullNoteList = notes
                filterNotes(binding.etSearchNotes.text?.toString().orEmpty())
            }
        }
    }

    private fun filterNotes(query: String) {
        val filtered = if (query.isBlank()) {
            fullNoteList
        } else {
            fullNoteList.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
        noteAdapter?.submitList(filtered)
    }

    private fun confirmDeleteNote(note: VaultNote) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_note_title)
            .setMessage(R.string.delete_note_message)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteNote(note) }
            .show()
    }
}
