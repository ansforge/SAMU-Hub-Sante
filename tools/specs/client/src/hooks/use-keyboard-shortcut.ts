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

export function useKeyboardShortcut({
  key,
  ctrlOrCmd,
  alt,
  shift,
  callback,
  allowInEditable,
}: ShortcutOptions) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const { key: pressed, ctrlKey, altKey, shiftKey, metaKey } = event;
      if (pressed.toLowerCase() !== key.toLowerCase()) {
        return;
      }
      if (!allowInEditable && isEditableTarget(event.target)) {
        return;
      }
      const cmdKeyPressed = isMac ? metaKey : ctrlKey;
      if (
        (ctrlOrCmd === undefined || ctrlOrCmd === cmdKeyPressed) &&
        (alt === undefined || alt === altKey) &&
        (shift === undefined || shift === shiftKey)
      ) {
        event.preventDefault();
        callback();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [key, ctrlOrCmd, alt, shift, callback, allowInEditable]);
  return { isMac };
}
