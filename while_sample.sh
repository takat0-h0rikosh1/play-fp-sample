#!/bin/bash

wait_until_port_opened () {
  while :
  do
    netstat -an | grep 9000 > /dev/null
    if [[ ! $? = 0 ]]; then
      echo "9000番ポートの開放を確認しました。無限ループを終了します。"
      break
    fi
  done
}

wait_until_port_opened
exit 0