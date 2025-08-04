/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import html2canvas, { type Options as HTML2CanvasOptions } from 'html2canvas';
import { useCallback, useRef, useState } from 'react';

const h2c = (html2canvas as any)?.default || html2canvas;

export type UseCurrentPng = [
  (callback?: BlobCallback) => Promise<string | undefined>,
  {
    isLoading: boolean;
    ref: React.RefObject<any>;
  },
];

/**
 * @param options - optional html2canvas Options object
 */
export function useCurrentPng(options?: Partial<HTML2CanvasOptions>): UseCurrentPng {
  const ref = useRef<SVGElement>(null);
  const [isLoading, setIsLoading] = useState(false);

  const getPng = useCallback(
    async (callback?: BlobCallback) => {
      if (ref.current?.parentElement) {
        setIsLoading(true);

        return await h2c(ref.current.parentElement, {
          logging: false,
          ...options,
        }).then((canvas: HTMLCanvasElement) => {
          if (callback) {
            canvas.toBlob(callback, 'image/png', 1.0);
          }
          setIsLoading(false);
          return canvas.toDataURL('image/png', 1.0);
        });
      }
    },
    [options],
  );

  return [
    getPng,
    {
      ref,
      isLoading,
    },
  ];
}
