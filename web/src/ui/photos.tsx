// Fotos anexadas a um registro. As imagens ficam só no IndexedDB deste navegador.

import { useCallback, useEffect, useRef, useState } from 'react';
import { repos } from '../data/repo';
import type { Photo, RecordType } from '../domain/types';
import { prepareStoredPhoto } from '../report/photoLoader';
import { showToast } from './components';
import { Icon } from './icons';

interface Added {
  key: string;
  blob: Blob;
  url: string;
}

interface Existing {
  photo: Photo;
  url: string;
}

export interface PhotoDraft {
  existing: Existing[];
  added: Added[];
  busy: boolean;
  add: (files: FileList | File[]) => Promise<void>;
  removeExisting: (photoId: number) => void;
  removeAdded: (key: string) => void;
  /** Grava as mudanças: apaga as removidas e salva as novas. */
  commit: (babyId: number, ownerId: number) => Promise<void>;
  count: number;
}

/** Rascunho das fotos de um registro: nada é gravado até chamar [commit]. */
export function usePhotoDraft(type: RecordType, ownerId: number | undefined): PhotoDraft {
  const [existing, setExisting] = useState<Existing[]>([]);
  const [added, setAdded] = useState<Added[]>([]);
  const [busy, setBusy] = useState(false);
  const urls = useRef<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    if (ownerId === undefined) return;
    void repos.photos.forOwner(type, ownerId).then((photos) => {
      if (cancelled) return;
      const items = photos.map((photo) => {
        const url = URL.createObjectURL(photo.blob);
        urls.current.push(url);
        return { photo, url };
      });
      setExisting(items);
    });
    return () => {
      cancelled = true;
    };
  }, [type, ownerId]);

  useEffect(
    () => () => {
      urls.current.forEach((u) => URL.revokeObjectURL(u));
      urls.current = [];
    },
    [],
  );

  const add = useCallback(async (files: FileList | File[]) => {
    setBusy(true);
    try {
      for (const file of Array.from(files)) {
        if (!file.type.startsWith('image/')) {
          showToast('Escolha apenas imagens.');
          continue;
        }
        try {
          const blob = await prepareStoredPhoto(file);
          const url = URL.createObjectURL(blob);
          urls.current.push(url);
          setAdded((list) => [...list, { key: `${Date.now()}-${Math.random()}`, blob, url }]);
        } catch {
          showToast('Não foi possível usar uma das fotos. Tente outra.');
        }
      }
    } finally {
      setBusy(false);
    }
  }, []);

  return {
    existing,
    added,
    busy,
    add,
    removeExisting: (photoId) => setExisting((list) => list.filter((e) => e.photo.id !== photoId)),
    removeAdded: (key) => setAdded((list) => list.filter((a) => a.key !== key)),
    commit: (babyId, id) =>
      repos.photos.sync(
        babyId,
        type,
        id,
        existing.map((e) => e.photo.id!),
        added.map((a) => a.blob),
      ),
    count: existing.length + added.length,
  };
}

export function PhotoAttachments({ draft, label = 'Fotos', hint = 'Opcional. As fotos ficam só neste aparelho.' }: { draft: PhotoDraft; label?: string; hint?: string }) {
  const input = useRef<HTMLInputElement>(null);
  return (
    <fieldset className="field">
      <legend>{label}</legend>
      <div className="photos">
        {draft.existing.map(({ photo, url }, i) => (
          <div className="photo" key={photo.id}>
            <img src={url} alt={`Foto ${i + 1}`} />
            <button type="button" aria-label={`Remover foto ${i + 1}`} onClick={() => draft.removeExisting(photo.id!)}>
              <Icon name="close" size={18} />
            </button>
          </div>
        ))}
        {draft.added.map((a, i) => (
          <div className="photo" key={a.key}>
            <img src={a.url} alt={`Foto nova ${i + 1}`} />
            <button type="button" aria-label={`Remover foto nova ${i + 1}`} onClick={() => draft.removeAdded(a.key)}>
              <Icon name="close" size={18} />
            </button>
          </div>
        ))}
      </div>
      <div>
        <input
          ref={input}
          type="file"
          accept="image/*"
          multiple
          className="sr-only"
          tabIndex={-1}
          aria-label="Escolher fotos neste aparelho"
          onChange={(e) => {
            if (e.target.files) void draft.add(e.target.files);
            e.target.value = '';
          }}
        />
        <button type="button" className="btn tonal small" onClick={() => input.current?.click()} disabled={draft.busy}>
          <Icon name="camera" size={20} />
          {draft.busy ? 'Preparando…' : 'Adicionar foto'}
        </button>
      </div>
      <span className="hint">{hint}</span>
    </fieldset>
  );
}
