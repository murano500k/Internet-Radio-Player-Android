#!/usr/bin/env bash
adb pull /storage/emulated/0/Android/data/com.stc.radio.player.debug/files/Pictures/error_log /tmp/irp_error_log
echo "log saved to /tmp/irp_error_log"