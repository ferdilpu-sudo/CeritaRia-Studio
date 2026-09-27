#!/usr/bin/env bash
set -euo pipefail

failed=0

while IFS= read -r -d '' file; do
  lines=$(wc -l < "$file")
  if [ "$lines" -gt 300 ]; then
    echo "::error file=$file::Handwritten Kotlin file has $lines lines; hard limit is 300."
    failed=1
  elif [ "$lines" -gt 250 ]; then
    echo "::warning file=$file::Handwritten Kotlin file has $lines lines; soft warning threshold is 250."
  fi
done < <(find app/src -type f -name '*.kt' -print0)

if grep -RniE 'service[_-]?role|R2_ACCESS_KEY_ID|R2_SECRET_ACCESS_KEY' app/src app/build.gradle.kts; then
  echo "::error::Privileged server credential identifier found in Android source."
  failed=1
fi

if grep -RniE '(Log\.[A-Za-z]+\(|println\(|print\().*(access[_-]?token|refresh[_-]?token|authorization|bearer|password)' app/src/main; then
  echo "::error::Potential sensitive credential logging found in Android source."
  failed=1
fi

if grep -RniE 'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}' app/src/main; then
  echo "::error::JWT-like literal found in Android source."
  failed=1
fi

exit "$failed"
