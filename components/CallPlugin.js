import { VisionCameraProxy, Frame } from 'react-native-vision-camera'
const plugin = VisionCameraProxy.initFrameProcessorPlugin('saran')

export function saran(frame) {
  'worklet'
  if (plugin == null) {
    throw new Error("Failed to load Frame Processor Plugin!")
  }
  return plugin.call(frame)
}

objects = [
  {
  boundingBox: {
    bottom: 480.8776550292969,
    left:  25.50100326538086,
    right: 624.090087890625,
    top: -3.8674163818359375
  },
  confidence:0.546,
  label:'false'}

]