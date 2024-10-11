#!/bin/sh

# Script will test the "-i" argument if...
#   * it contains "sleep" then if so the script will sleep for 5 seconds and then exit normally
#   * it contains "error" then it exists with error code 1
#   * else it just exists right away with exit code 0

# Arguments
while [ $# -gt 0 ]
do
  key="$1"
  case $key in
    -i)
      input="$2"
      shift # past argument
      shift # past value
      ;;
    *)
      # ignored
      shift
      ;;
  esac
done

# Functions
containsAny() {
  value=$1
  shift
  for arg in "$@"
  do
    case $value in
      *"$arg"*)
        echo true
        exit 0
        ;;
    esac
  done
  echo false
}


# Script
if [ $(containsAny "${input}" "sleep") = true ]; then
  sleep 10
elif [ $(containsAny "${input}" "error") = true ]; then
  exit 1
fi

exit 0
