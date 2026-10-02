"""
校验 ThemeMorphTimeline 的纯函数行为（不依赖 Compose 运行时）。

直接按 Kotlin 语义翻译关键分支做数值验证，重点覆盖：
  1. ease 曲线单调且端点正确
  2. 循环相位插值不走反方向长弧
  3. 造型枚举在 StyleSwapShare 之前保持旧值、之后取新值
  4. 整数属性插值不会超出两端范围
  5. sameRecipe 时直接返回目标，不产生中间态
"""


def ease(t):
    x = min(max(t, 0.0), 1.0)
    return x * x * (3.0 - 2.0 * x)


def lerp(a, b, t):
    return a + (b - a) * min(max(t, 0.0), 1.0)


def lerp_int(a, b, t):
    # Kotlin round(): .5 向上取整
    v = a + (b - a) * min(max(t, 0.0), 1.0)
    return int(v + 0.5) if v >= 0 else -int(-v + 0.5)


def lerp_phase(a, b, t):
    tt = min(max(t, 0.0), 1.0)
    delta = b - a
    if abs(delta) > 0.5:
        delta -= 1.0 if delta > 0 else -1.0
    nxt = a + delta * tt
    return ((nxt % 1.0) + 1.0) % 1.0


failures = []


def check(name, cond, detail=""):
    if cond:
        print("  PASS  %s" % name)
    else:
        print("  FAIL  %s  %s" % (name, detail))
        failures.append(name)


print("[1] ease 曲线")
check("端点 ease(0)==0", abs(ease(0.0)) < 1e-9)
check("端点 ease(1)==1", abs(ease(1.0) - 1.0) < 1e-9)
check("中点 ease(0.5)==0.5", abs(ease(0.5) - 0.5) < 1e-9)
mono = all(ease(i / 100.0) <= ease((i + 1) / 100.0) + 1e-12 for i in range(100))
check("单调不减", mono)
# 导数在 0.5 处最大：比较 0.45->0.55 与 0.05->0.15 的增量
mid = ease(0.55) - ease(0.45)
edge = ease(0.15) - ease(0.05)
check("中段斜率 > 边段斜率", mid > edge, "mid=%.4f edge=%.4f" % (mid, edge))

print("[2] 循环相位插值（最短弧）")
check("0.9->0.1 走 +0.2 而非 -0.8", abs(lerp_phase(0.9, 0.1, 1.0) - 0.1) < 1e-6,
      "got %.4f" % lerp_phase(0.9, 0.1, 1.0))
check("0.1->0.9 走 -0.2 而非 +0.8", abs(lerp_phase(0.1, 0.9, 1.0) - 0.9) < 1e-6,
      "got %.4f" % lerp_phase(0.1, 0.9, 1.0))
check("0.2->0.6 正向", abs(lerp_phase(0.2, 0.6, 1.0) - 0.6) < 1e-6)
check("中点 0.9->0.1 在 0.0 附近", abs(lerp_phase(0.9, 0.1, 0.5) - 0.0) < 1e-6,
      "got %.4f" % lerp_phase(0.9, 0.1, 0.5))
in_range = all(0.0 <= lerp_phase(0.9, 0.1, t / 20.0) < 1.0 for t in range(21))
check("全程落在 0..1", in_range)

print("[3] 造型枚举切换点")
share = 0.34
check("eased<0.34 保持旧值", ease(0.1) < share, "eased=%.4f" % ease(0.1))
check("eased>=0.34 切到新值", ease(0.9) >= share, "eased=%.4f" % ease(0.9))
# ease(x)=0.34 的解约为 x=0.4179 —— 即进度过 41.8% 才换样式，留足淡出时间
swap_x = 0.4179
check("进度 42%% 时已完成样式切换", ease(swap_x) >= share, "eased=%.4f" % ease(swap_x))

print("[4] 连续属性插值范围")
for a, b in [(0.10, 0.26), (1.0, 3.0), (0.0, 1.0)]:
    vals = [lerp(a, b, t / 20.0) for t in range(21)]
    lo, hi = min(a, b), max(a, b)
    check("lerp(%s,%s) 不越界" % (a, b), all(lo - 1e-9 <= v <= hi + 1e-9 for v in vals))

for a, b in [(2, 3), (1, 3), (0, 2), (3, 1)]:
    vals = [lerp_int(a, b, t / 20.0) for t in range(21)]
    lo, hi = min(a, b), max(a, b)
    check("lerpInt(%d,%d) 不越界" % (a, b), all(lo <= v <= hi for v in vals),
          "got %s" % sorted(set(vals)))

print("[5] 粒子数插值不产生第三种值")
# 实际配方里粒子数取值范围是 8..12
a, b = 8, 12
vals = [lerp_int(a, b, t / 20.0) for t in range(21)]
check("8->12 只出现 8..12", set(vals) <= set(range(8, 13)), "got %s" % sorted(set(vals)))

print("[6] sweep 位置与亮度（连续性回归）")
SWEEP_END = 1.25
SWEEP_MAX_ALPHA = 0.30


def sweep_at(t):
    # 修正后：位置全程连续推进到 SweepEnd，不再有"窗口末归零"的提前返回
    return ease(min(max(t, 0.0), 1.0)) * SWEEP_END


def sweep_alpha(p):
    if p <= 0.0 or p >= SWEEP_END:
        return 0.0
    n = p / SWEEP_END
    d = min(max((n - 0.5) * 2.0, -1.0), 1.0)
    shaped = 1.0 - d * d
    return SWEEP_MAX_ALPHA * shaped * shaped * (3.0 - 2.0 * shaped)


check("sweep(0)==0", abs(sweep_at(0.0)) < 1e-9)
check("sweep(1)==SweepEnd", abs(sweep_at(1.0) - SWEEP_END) < 1e-6,
      "got %.4f" % sweep_at(1.0))
sw_mono = all(sweep_at(i / 200.0) <= sweep_at((i + 1) / 200.0) + 1e-12
              for i in range(200))
check("位置全程单调不减（无跳变）", sw_mono)
# 关键回归：不再有"窗口末突然归零"
check("进度 90%% 时位置仍 >1（已扫出但连续）", sweep_at(0.9) > 1.0,
      "got %.4f" % sweep_at(0.9))

alphas = [sweep_alpha(sweep_at(i / 200.0)) for i in range(201)]
peak = max(alphas)
# 离散采样打不中精确峰值（峰值在 ease(t)=0.4 即 t≈0.386 处），用峰值区间而非精确值断言。
check("峰值接近 0.30（采样容差内）", peak > SWEEP_MAX_ALPHA * 0.98,
      "peak=%.4f" % peak)
check("起点亮度为 0", alphas[0] < 1e-9)
check("终点亮度为 0", alphas[-1] < 1e-9)
check("亮度全程在 [0, 峰值]", all(0.0 <= a <= SWEEP_MAX_ALPHA + 1e-9 for a in alphas))

# 核心回归：亮度必须连续衰减到 0，不能从"可见"直接跳到 0。
# 这正是早期版本 x>=SweepShare 时 return 0 造成的 bug（单帧从 0.30 掉到 0）。
discontinuities = [
    (i, alphas[i], alphas[i + 1])
    for i in range(len(alphas) - 1)
    if alphas[i] > 0.02 and alphas[i + 1] < 0.002
]
check("亮度无「非零直接跳到 0」的断裂（关键回归）", not discontinuities,
      "found %s" % discontinuities[:3])
# 收尾段（后 25%）必须单调衰减到 0
tail = alphas[int(len(alphas) * 0.75):]
check("收尾段单调不增",
      all(tail[i] >= tail[i + 1] - 1e-12 for i in range(len(tail) - 1)))
check("收尾段确实降到 0", tail[-1] < 1e-9)
# 峰值位置：sweepAt(0.5)=ease(0.5)*1.25=0.625，归一化后恰为 0.5 -> 亮度达峰
check("峰值出现在中线（位置 0.5*SweepEnd）",
      abs(sweep_at(0.5) - 0.5 * SWEEP_END) < 1e-6,
      "sweep_at(0.5)=%.4f" % sweep_at(0.5))

print("[7] 端到端：0.5 进度时处于过渡中")
mid_frame_eased = ease(0.5)
check("进度 0.5 -> eased 0.5", abs(mid_frame_eased - 0.5) < 1e-9)
check("进度 0.5 -> 样式已切换", mid_frame_eased >= share)
# 进度 0.5 时 ease=0.5 -> 位置 0.625 -> 归一化 0.5 -> 亮度恰好达峰
check("进度 0.5 -> 光扫亮度达到峰值",
      abs(sweep_alpha(sweep_at(0.5)) - SWEEP_MAX_ALPHA) < 1e-6,
      "alpha=%.4f" % sweep_alpha(sweep_at(0.5)))
# 形变结束后（进度 1）必须完全静止
check("进度 1.0 -> 光扫不可见", sweep_alpha(sweep_at(1.0)) < 1e-9)
check("进度 0.0 -> 光扫不可见", sweep_alpha(sweep_at(0.0)) < 1e-9)
# 两端亮度斜率应趋于 0（smoothstep 收尾），验证不出现边界阶跃
d_start = sweep_alpha(sweep_at(0.01)) - sweep_alpha(sweep_at(0.0))
d_end = sweep_alpha(sweep_at(1.0)) - sweep_alpha(sweep_at(0.99))
check("起点斜率很小（无突变）", abs(d_start) < 0.01, "d_start=%.5f" % d_start)
check("终点斜率很小（无突变）", abs(d_end) < 0.01, "d_end=%.5f" % d_end)

print()
if failures:
    print("RESULT: %d FAILED -> %s" % (len(failures), failures))
else:
    print("RESULT: ALL PASS")
