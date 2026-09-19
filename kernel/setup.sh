#!/bin/sh
set -eu

GKI_ROOT=$(pwd)

# Where this script lives — i.e. <XinovaSU>/kernel when run from a checkout.
BUNDLED_KERNEL=$(cd "$(dirname "$0")" && pwd)

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag>]"
    echo "  --cleanup:              Cleans up previous modifications made by the script."
    echo "  <commit-or-tag>:        Only used when falling back to a git clone."
    echo "  -h, --help:             Displays this usage information."
    echo "  (no args):              Integrates the bundled kernel/ (or clones main)."
}

initialize_variables() {
    # Layout differs by vendor:
    #   AOSP GKI      : <root>/common/drivers
    #   OPLUS / 一加   : <root>/kernel_platform/common/drivers
    #   older trees   : <root>/drivers
    for cand in \
        "$GKI_ROOT/common/drivers" \
        "$GKI_ROOT/kernel_platform/common/drivers" \
        "$GKI_ROOT/kernel_platform/msm-kernel/drivers" \
        "$GKI_ROOT/drivers"; do
        if test -d "$cand"; then
            DRIVER_DIR="$cand"
            break
        fi
    done

    if [ -z "${DRIVER_DIR:-}" ]; then
        echo '[ERROR] "drivers/" directory not found. Looked in:'
        echo '          common/drivers'
        echo '          kernel_platform/common/drivers   (OPLUS / 一加)'
        echo '          kernel_platform/msm-kernel/drivers'
        echo '          drivers/'
        echo '        Run this script from the tree root (the dir containing kernel_platform/ or common/).'
        exit 127
    fi

    echo "[+] drivers dir: $DRIVER_DIR"
    DRIVER_MAKEFILE=$DRIVER_DIR/Makefile
    DRIVER_KCONFIG=$DRIVER_DIR/Kconfig
}

# Reverts modifications made by this script
perform_cleanup() {
    echo "[+] Cleaning up..."
    [ -L "$DRIVER_DIR/kernelsu" ] && rm "$DRIVER_DIR/kernelsu" && echo "[-] Symlink removed."
    grep -q "kernelsu" "$DRIVER_MAKEFILE" && sed -i '/kernelsu/d' "$DRIVER_MAKEFILE" && echo "[-] Makefile reverted."
    grep -q "drivers/kernelsu/Kconfig" "$DRIVER_KCONFIG" && sed -i '/drivers\/kernelsu\/Kconfig/d' "$DRIVER_KCONFIG" && echo "[-] Kconfig reverted."
    if [ -d "$GKI_ROOT/XinovaSU" ]; then
        rm -rf "$GKI_ROOT/XinovaSU" && echo "[-] Cloned tree deleted."
    fi
}

# Integrates XinovaSU into the kernel tree.
#
# IMPORTANT: upstream KernelSU's setup.sh git-clones tiann/KernelSU here. Doing
# that in this repo silently integrates *upstream KernelSU* instead of XinovaSU
# — you get a kernel with none of XinovaSU's features (Hide/Umount/KernelSpoof/
# PathHide/NetIsolate/CpuSpoof) and no error to tell you. So: always prefer the
# kernel/ directory this script was shipped in, and only clone as a fallback
# for the `curl | bash` style invocation.
setup_xinovasu() {
    if [ -f "$BUNDLED_KERNEL/Kbuild" ]; then
        XNSU_SRC="$BUNDLED_KERNEL"
        echo "[+] Using bundled kernel/ at $XNSU_SRC"
    else
        echo "[+] No bundled kernel/ next to this script; cloning XinovaSU..."
        test -d "$GKI_ROOT/XinovaSU" || git clone https://github.com/kusesad-1122/XinovaSU && echo "[+] Repository cloned."
        cd "$GKI_ROOT/XinovaSU"
        if [ -z "${1-}" ]; then
            git checkout main && echo "[-] Checked out main."
        else
            git checkout "$1" && echo "[-] Checked out $1."
        fi
        XNSU_SRC="$GKI_ROOT/XinovaSU/kernel"
    fi

    cd "$DRIVER_DIR"
    ln -sf "$(realpath --relative-to="$DRIVER_DIR" "$XNSU_SRC")" "kernelsu" && echo "[+] Symlink created."

    # Add entries in Makefile and Kconfig if not already existing.
    # CONFIG_KSU matches kernel/Kconfig's `config KSU` — keep them in sync.
    grep -q "kernelsu" "$DRIVER_MAKEFILE" || printf "\nobj-\$(CONFIG_KSU) += kernelsu/\n" >> "$DRIVER_MAKEFILE" && echo "[+] Modified Makefile."
    grep -q "source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" || sed -i "/endmenu/i\source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" && echo "[+] Modified Kconfig."
    echo '[+] Done.'
}

# Process command-line arguments
if [ "$#" -eq 0 ]; then
    initialize_variables
    setup_xinovasu
elif [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    display_usage
elif [ "$1" = "--cleanup" ]; then
    initialize_variables
    perform_cleanup
else
    initialize_variables
    setup_xinovasu "$@"
fi
