package top.yudoge.phoneclaw.llm.skills

import android.content.Context
import android.util.Log
import java.io.File

object SkillSynchronizer {
    private const val TAG = "SkillSynchronizer"
    private const val ASSETS_SKILLS_DIR = "skills"

    data class SyncResult(
        val added: Int = 0,
        val updated: Int = 0,
        val skipped: Int = 0
    ) {
        val hasChanges: Boolean get() = added > 0 || updated > 0
        
        fun logSummary(): String {
            return "added=$added, updated=$updated, skipped=$skipped"
        }
    }

    fun syncSkillsFromAssets(context: Context, skillsDir: File): SyncResult {
        var added = 0
        var updated = 0
        var skipped = 0

        try {
            val assetManager = context.assets
            val skillDirs = assetManager.list(ASSETS_SKILLS_DIR)?.filter { it.isNotEmpty() } ?: emptyList()
            
            if (skillDirs.isEmpty()) {
                Log.d(TAG, "No skills found in assets")
                return SyncResult()
            }

            if (!skillsDir.exists()) {
                skillsDir.mkdirs()
            }

            val repo = FileBasedSkillRepository(skillsDir)

            for (skillDirName in skillDirs) {
                try {
                    val destDir = File(skillsDir, skillDirName)
                    val destSkillMd = File(destDir, "SKILL.md")
                    
                    val assetSkillMdPath = "$ASSETS_SKILLS_DIR/$skillDirName/SKILL.md"
                    val assetContent = assetManager.open(assetSkillMdPath).bufferedReader().readText()
                    
                    if (!destDir.exists()) {
                        destDir.mkdirs()
                        destSkillMd.writeText(assetContent)
                        
                        copySupportingFiles(context, skillDirName, destDir)
                        
                        added++
                        Log.d(TAG, "Added skill: $skillDirName")
                    } else {
                        val existingContent = destSkillMd.readText()
                        if (existingContent != assetContent) {
                            destSkillMd.writeText(assetContent)
                            
                            copySupportingFiles(context, skillDirName, destDir)
                            
                            updated++
                            Log.d(TAG, "Updated skill: $skillDirName")
                        } else {
                            skipped++
                            Log.d(TAG, "Skipped skill (unchanged): $skillDirName")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing skill $skillDirName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing skills in assets", e)
        }

        return SyncResult(added, updated, skipped)
    }

    private fun copySupportingFiles(context: Context, skillDirName: String, destDir: File) {
        try {
            val assetManager = context.assets
            val assetSkillDir = "$ASSETS_SKILLS_DIR/$skillDirName"
            val allFiles = assetManager.list(assetSkillDir)?.toList() ?: emptyList()
            
            for (fileName in allFiles) {
                if (fileName == "SKILL.md") continue
                
                try {
                    val srcPath = "$assetSkillDir/$fileName"
                    val destFile = File(destDir, fileName)
                    
                    assetManager.open(srcPath).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Copied supporting file: $fileName for skill: $skillDirName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying supporting file $fileName for skill $skillDirName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing supporting files for skill $skillDirName", e)
        }
    }
}
