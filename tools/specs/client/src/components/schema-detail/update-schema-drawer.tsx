import { DiffEditor, type DiffOnMount } from "@monaco-editor/react";
import { useRef, useState } from "react";
import { useSchemaStore } from "@/store/schema-store";
import { Button } from "../ui/button";
import { Sheet, SheetContent, SheetHeader } from "../ui/sheet";
import { useUpdateSchema } from "@/hooks/use-update-schema";
import { getRouteApi, useRouter } from "@tanstack/react-router";
import { toast } from "../ui/toast";
import { defaultWorkingBranch } from "@/config";

const schemaRouteApi = getRouteApi("/$schemaName");
const rootRouteApi = getRouteApi("__root__");

type UpdateSchemaDrawerProps = {
  rawText?: string;
};

export const UpdateSchemaDrawer = ({
  rawText: originalValue,
}: UpdateSchemaDrawerProps) => {
  const schemaUpdateActive = useSchemaStore((s) => s.schemaUpdateActive);
  const closeUpdateSchemaDrawer = useSchemaStore(
    (s) => s.closeUpdateSchemaDrawer,
  );

  const editorValueRef = useRef<string>("");
  const [isDirty, setIsDirty] = useState(false);

  const handleDiffEditorMount: DiffOnMount = (editor) => {
    const modifiedEditor = editor.getModifiedEditor();
    editorValueRef.current = modifiedEditor.getValue();
    modifiedEditor.onDidChangeModelContent(() => {
      const value = modifiedEditor.getValue();
      editorValueRef.current = value;
      setIsDirty((prev) => {
        const next = value !== originalValue;
        return prev === next ? prev : next;
      });
    });
  };

  return (
    <Sheet
      open={schemaUpdateActive}
      onOpenChange={(open: boolean) => !open && closeUpdateSchemaDrawer()}
    >
      <SheetContent className={"w-full max-w-4xl!"}>
        <SheetHeader className="border-b">edit</SheetHeader>
        <div className="min-h-0 flex-1 overflow-y-auto w-full">
          {originalValue !== undefined && (
            <DiffEditor
              height="100%"
              language="json"
              original={originalValue}
              modified={originalValue}
              onMount={handleDiffEditorMount}
            />
          )}
        </div>
        <div className="border-t p-6">
          <UpdateSchemaActions isDirty={isDirty} valueRef={editorValueRef} />
        </div>
      </SheetContent>
    </Sheet>
  );
};

type UpdateSchemaActions = {
  isDirty: boolean;
  valueRef: React.RefObject<string>;
};
const UpdateSchemaActions = ({ isDirty, valueRef }: UpdateSchemaActions) => {
  const [commitMessage, setCommitMessage] = useState("");
  const { ref: branch } = rootRouteApi.useSearch();
  const { schemaName } = schemaRouteApi.useParams();
  const router = useRouter();

  const { mutateAsync, isPending } = useUpdateSchema();

  const handleSubmit = () => {
    const value = valueRef.current;
    try {
      JSON.parse(value);
    } catch {
      toast.add({ type: "error", title: "JSON invalide." });
      return;
    }

    toast.promise(
      mutateAsync({
        body: {
          data: value,
          ref: defaultWorkingBranch,
          commit_message: commitMessage,
          new_branch: null,
        },
        schemaId: schemaName,
      }).then(async (res) => {
        // we just wrote fresh bytes ourselves — refresh the route loader
        // (drawer baseline + page both read from it) so it doesn't keep
        // serving what was there before the commit
        await router.invalidate();
        return res;
      }),
      {
        loading: "Mise à jour du schéma…",
        success: (data) => `Commit créé : ${data.commit_url}`,
        error: (err) => err.message ?? "Échec de la mise à jour.",
      },
    );
  };

  return (
    <div className="flex flex-col gap-6">
      <textarea
        className="w-full min-h-16 rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        placeholder="Message de commit"
        value={commitMessage}
        onChange={(e) => setCommitMessage(e.target.value)}
      />
      <div className="flex gap-2 justify-between">
        <Button
          disabled={!isDirty || !commitMessage || !branch || isPending}
          onClick={handleSubmit}
        >
          {isPending ? "Commiting..." : "Commit"}
        </Button>
      </div>
    </div>
  );
};
