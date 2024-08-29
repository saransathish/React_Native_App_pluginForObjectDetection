import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Camera, runAsync, useCameraFormat, useFrameProcessor, useSkiaFrameProcessor } from 'react-native-vision-camera';
import { saran } from './CallPlugin';
import { Worklets } from 'react-native-worklets-core';
import { Canvas, PaintStyle, Rect, Skia } from '@shopify/react-native-skia';

interface FrameProcessingProps {
  device: any;
}

interface Frames {
  bottom: number;
  left: number;
  right: number;
  top: number;
}

interface Detected {
  boundingBox: Frames;
  confidence: number;
  label: string;
}
const imageWidth = 300;
const imageHeight = 300;

const FrameProcessing: React.FC<FrameProcessingProps> = ({ device }) => {
  const [objects, setObjects] = useState<Detected[]>([]);
  
  const format = useCameraFormat(device, [
    { videoResolution: { width: imageWidth, height: imageHeight } },
    { fps: 15 }
  ])

  const onDetected = Worklets.createRunOnJS((detectedObjects: Detected[]) => {
    setObjects(detectedObjects);
  });

  const paint = Skia.Paint();
  paint.setStyle(PaintStyle.Fill);
  paint.setStrokeWidth(2);
  paint.setColor(Skia.Color('red'));

  const frameProcessor = useSkiaFrameProcessor((frame) => {
    'worklet';


    runAsync(frame , () => {
      'worklet'
       const values:Detected[] = saran(frame) as [];
       console.log(values)
       onDetected(values);
    })
    frame.drawRect(Skia.XYWHRect(100, 100, 200, 200), paint);


    frame.render(paint)
    
  }, [onDetected,paint]);

  return (
    <View style={{ flex: 1 }}>
      <Camera
        style={{ flex: 1 }}
        device={device}
        isActive={true}

        frameProcessor={frameProcessor}
        pixelFormat="yuv"
        // format={format}
      />

      {objects.map(({ boundingBox, label, confidence }, index) => {
        const { top, left, right, bottom } = boundingBox;

        return (
          <View
            key={index}
            style={[
              styles.detectionFrame,
              {
                top: top,
                left: left,
                width: right - left,
                height: bottom - top,
              },
            ]}
          >
            <View style={styles.detectionFrameLabel}>
              <Text style={styles.labelText}>{`${label} (${(confidence * 100).toFixed(2)}%)`}</Text>
            </View>
          </View>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  detectionFrame: {
    position: 'absolute',
    borderWidth: 6,
    borderColor: '#00ff00',
    zIndex: 9,
  },
  detectionFrameLabel: {
    position: 'absolute',
    backgroundColor: 'rgba(0, 255, 0, 0.5)',
    padding: 2,
    borderRadius: 3,
    top: 0,
    left: 0,
  },
  labelText: {
    color: '#fff',
    fontSize: 12,
  },
});

export default FrameProcessing;
