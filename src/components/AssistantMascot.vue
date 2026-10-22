<template>
  <view class="mascot" :class="{ 'mascot-awake': awake, 'mascot-thinking': thinking }" :style="{ fontSize: size + 'px' }" aria-hidden="true">
    <view class="mascot-shadow"></view>
    <view class="mascot-character">
      <view class="mascot-antenna"><view></view></view>
      <view class="mascot-ear mascot-ear-left"></view><view class="mascot-ear mascot-ear-right"></view>
      <view class="mascot-head">
        <view class="mascot-shine"></view>
        <view class="mascot-face">
          <view class="mascot-eye mascot-eye-left"></view><view class="mascot-eye mascot-eye-right"></view>
          <view class="mascot-smile"></view>
          <view class="mascot-cheek mascot-cheek-left"></view><view class="mascot-cheek mascot-cheek-right"></view>
        </view>
      </view>
      <view class="mascot-arm mascot-arm-left"></view><view class="mascot-arm mascot-arm-right"></view>
      <view class="mascot-body"><view class="mascot-heart"></view></view>
      <view class="mascot-foot mascot-foot-left"></view><view class="mascot-foot mascot-foot-right"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: number; awake?: boolean; thinking?: boolean }>(), { size: 72, awake: false, thinking: false })
</script>

<style scoped>
.mascot { position: relative; width: 1em; height: 1.12em; pointer-events: none; }
.mascot-character { position: absolute; inset: 0; transform-origin: 50% 65%; animation: mascot-float 3.2s ease-in-out infinite; }
.mascot-shadow { position: absolute; bottom: .01em; left: .18em; width: .64em; height: .08em; border-radius: 50%; background: #305479; opacity: .14; filter: blur(3px); animation: mascot-shadow 3.2s ease-in-out infinite; }
.mascot-antenna { position: absolute; left: .49em; top: .04em; width: .035em; height: .15em; background: #9bbfd4; transform: rotate(12deg); }
.mascot-antenna > view { position: absolute; top: -.035em; left: -.035em; width: .11em; height: .11em; border: .025em solid #efffff; border-radius: 50%; background: #67d8c0; box-shadow: 0 0 .13em #92ead7; }
.mascot-head { position: absolute; z-index: 3; left: .09em; top: .19em; width: .82em; height: .61em; overflow: hidden; border-radius: .27em .27em .23em .23em; background: linear-gradient(145deg,#fff 40%,#e3eff9); box-shadow: inset -.025em -.035em .045em #c0d8e9, inset .025em .025em .03em #fff, 0 .04em .08em #355c7c26; }
.mascot-shine { position: absolute; top: .035em; left: .18em; width: .35em; height: .04em; border-radius: 50%; background: white; }
.mascot-face { position: absolute; left: .095em; top: .12em; width: .63em; height: .35em; border: .025em solid #b7cfe0; box-sizing: border-box; border-radius: .15em .15em .13em .13em; background: linear-gradient(145deg,#234960,#123248); box-shadow: inset 0 .025em .06em #102333; }
.mascot-eye { position: absolute; top: .09em; width: .065em; height: .09em; border-radius: .05em; background: #9ff3e0; box-shadow: 0 0 .045em #9ff3e077; animation: mascot-blink 6.4s infinite; }
.mascot-eye-left { left: .145em; }.mascot-eye-right { right: .145em; }
.mascot-smile { position: absolute; left: .255em; top: .17em; width: .075em; height: .045em; border: .018em solid #9ff3e0; border-top: 0; border-radius: 0 0 .06em .06em; }
.mascot-cheek { position: absolute; top: .2em; width: .075em; height: .028em; border-radius: 50%; background: #7ea5b8; opacity: .55; }
.mascot-cheek-left { left: .065em; }.mascot-cheek-right { right: .065em; }
.mascot-ear { position: absolute; top: .39em; width: .08em; height: .18em; border-radius: .05em; background: #6faed4; box-shadow: inset 0 .025em .02em #cce9fa; }
.mascot-ear-left { left: .04em; }.mascot-ear-right { right: .04em; }
.mascot-body { position: absolute; z-index: 2; top: .72em; left: .3em; width: .4em; height: .27em; border-radius: .08em .08em .14em .14em; background: linear-gradient(110deg,#fff,#d8eaf6); box-shadow: inset -.02em -.025em .025em #aecde4; }
.mascot-heart { position: absolute; left: .17em; top: .105em; width: .075em; height: .075em; transform: rotate(-45deg); background: #ef9b95; border-radius: .015em; }
.mascot-heart::before,.mascot-heart::after { content: ''; position: absolute; width: .075em; height: .075em; border-radius: 50%; background: #ef9b95; }
.mascot-heart::before { top: -.04em; }.mascot-heart::after { left: .04em; }
.mascot-arm { position: absolute; z-index: 1; top: .75em; width: .105em; height: .19em; border-radius: .06em; background: linear-gradient(#edf8ff,#a8cfe6); }
.mascot-arm-left { left: .2em; transform: rotate(30deg); }.mascot-arm-right { right: .18em; transform: rotate(-40deg); transform-origin: top center; animation: mascot-idle-wave 6s ease-in-out infinite; }
.mascot-awake .mascot-arm-right { animation: mascot-wave 1.8s ease-in-out infinite; }
.mascot-foot { position: absolute; top: .96em; width: .13em; height: .075em; border-radius: .04em .04em .03em .03em; background: #83b5d4; }
.mascot-foot-left { left: .33em; }.mascot-foot-right { right: .33em; }
.mascot-thinking .mascot-eye { animation: mascot-think 1s ease-in-out infinite alternate; }.mascot-thinking .mascot-eye-right { animation-delay: .3s; }
@keyframes mascot-float { 0%,100% { transform: translateY(0) rotate(-2deg); } 50% { transform: translateY(-.11em) rotate(2deg); } }
@keyframes mascot-shadow { 0%,100% { transform: scaleX(1); opacity: .16; } 50% { transform: scaleX(.72); opacity: .08; } }
@keyframes mascot-blink { 0%,43%,47%,100% { transform: scaleY(1); } 45% { transform: scaleY(.15); } }
@keyframes mascot-idle-wave { 0%,38%,100% { transform: rotate(-40deg); } 10%,28% { transform: rotate(-110deg); } 19% { transform: rotate(-65deg); } }
@keyframes mascot-wave { 0%,100% { transform: rotate(-40deg); } 30%,70% { transform: rotate(-100deg); } 50% { transform: rotate(-70deg); } }
@keyframes mascot-think { from { opacity: .4; transform: scaleY(.6); } to { opacity: 1; transform: scaleY(1); } }
@media (prefers-reduced-motion: reduce) { .mascot-character,.mascot-shadow,.mascot-eye,.mascot-arm-right,.mascot-awake .mascot-arm-right,.mascot-thinking .mascot-eye { animation: none; } }
</style>
