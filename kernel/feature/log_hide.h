#ifndef __XNSU_H_LOG_HIDE
#define __XNSU_H_LOG_HIDE

/*
 * Kernel-side log suppression.
 *
 * SKRoot Pro suppresses SELinux denial records before they reach the audit
 * ring buffer. Two independent surfaces are covered:
 *
 *   audit_log_start  -- entry point of every audit record. Stubbed to return
 *                       NULL, which makes every audit_log_* caller bail out.
 *   avc_denied       -- the LSM hook that emits "avc: denied". Stubbed to
 *                       return 0 (handled) so no AVC record is produced.
 *
 * Both are optional: a symbol that cannot be resolved (KMI variance, CONFIG
 * differences) is skipped with a warning and the rest still works. Nothing is
 * patched when the feature is off, and patching is only ever attempted once
 * the feature is explicitly enabled.
 *
 * Kernel text is written through xnsu_patch_text(), the same stop_machine
 * path already used by the syscall hook, so the write is done on all CPUs
 * with the instruction cache flushed.
 *
 * NOTE: This is intentionally narrower than SKRoot. We only neutralise the
 * two record producers; we do not touch the audit ring buffer, dmesg, or
 * console loglevel. Suppressing those needs a different (much riskier)
 * approach and is out of scope for a drop-in feature.
 */

int xnsu_log_hide_add_symbol(const char *symbol_name, u16 ret_mode);
int xnsu_log_hide_remove_symbol(const char *symbol_name);
void xnsu_log_hide_clear_symbols(void);

void xnsu_log_hide_init(void);
void xnsu_log_hide_exit(void);

#endif // __XNSU_H_LOG_HIDE
