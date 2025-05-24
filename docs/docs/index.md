---
hide_comments: true
title: ☕ jAI Workflows
---
<script>
  // This script only runs in MkDocs, not on GitHub
  var hideGitHubVersion = function() {
    document.querySelectorAll('.github-only').forEach(el => el.style.display = 'none');
  };

  // Handle both initial load and subsequent navigation
  document.addEventListener('DOMContentLoaded', hideGitHubVersion);
  document$.subscribe(hideGitHubVersion);
</script>

--8<-- "https://raw.githubusercontent.com/czelabueno/jai-workflow/refs/heads/main/README.md"

