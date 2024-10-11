# Script will test the "-i" argument if...
#   * it contains "sleep" then if so the script will sleep for 5 seconds and then exit normally
#   * it contains "error" then it exists with error code 1
#   * else it just exists right away with exit code 0

for ($i = 0; $i -le $args.Length; $i++) {
  if ($args[$i] -eq "-i") {
	$v = $args[$i + 1];
	if ($v -Match "sleep") {
	  Start-Sleep -Seconds 10
	} elseif ($v -Match "error") {
	  exit 1
	}
  }
}

exit 0
