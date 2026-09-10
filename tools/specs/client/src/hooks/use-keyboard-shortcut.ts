import { isMac } from "@/lib/utils";
import { useEffect } from "react";

type ShortcutOptions = {
  key: string;
  ctrlOrCmd?: boolean;
  alt?: boolean;
  shift?: boolean;
  callback: () => void;
  // let shortcut fire while focus is in an input/textarea/contenteditable
  allowInEditable?: boolean;
};

function isEditableTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  return (
    tag === "INPUT" ||
    tag === "TEXTAREA" ||
    tag === "SELECT" ||
    target.isContentEditable
  );
}

export function useKeyboardShortcut(options: ShortcutOptions) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const { key, ctrlKey, altKey, shiftKey, metaKey } = event;
      if (key.toLowerCase() !== options.key.toLowerCase()) {
        return;
      }
      if (!options.allowInEditable && isEditableTarget(event.target)) {
        return;
      }
      const cmdKeyPressed = isMac ? metaKey : ctrlKey;
      if (
        (options.ctrlOrCmd === undefined ||
          options.ctrlOrCmd === cmdKeyPressed) &&
        (options.alt === undefined || options.alt === altKey) &&
        (options.shift === undefined || options.shift === shiftKey)
      ) {
        event.preventDefault();
        options.callback();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [options]);
  return { isMac };
}
