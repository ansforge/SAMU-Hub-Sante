import { DiffEditor, type DiffOnMount } from "@monaco-editor/react";
import { useRef, useState } from "react";
import { useSchemaStore } from "@/store/schema-store";
import { Button } from "../ui/button";
import { Sheet, SheetContent, SheetHeader } from "../ui/sheet";
import { useUpdateSchema } from "@/hooks/use-update-schema";
import { getRouteApi, useRouter } from "@tanstack/react-router";
import { toast } from "../ui/toast";
import { defaultWorkingBranch } from "@/config";
import { Alert, AlertDescription, AlertTitle } from "../ui/alert";
import { TriangleAlert } from "lucide-react";

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

  const { schemaName } = schemaRouteApi.useParams();

  const editorValueRef = useRef<string>("");
  const [changeCount, setChangeCount] = useState(0);
  const [jsonError, setJsonError] = useState<string | null>(null);

  const validate = (value: string) => {
    try {
      JSON.parse(value);
      setJsonError(null);
    } catch {
      setJsonError("Le JSON saisi n'est pas valide.");
    }
  };

  const handleDiffEditorMount: DiffOnMount = (editor) => {
    const modifiedEditor = editor.getModifiedEditor();
    editorValueRef.current = modifiedEditor.getValue();
    validate(editorValueRef.current);
    modifiedEditor.onDidChangeModelContent(() => {
      editorValueRef.current = modifiedEditor.getValue();
      validate(editorValueRef.current);
    });
    editor.onDidUpdateDiff(() => {
      setChangeCount(editor.getLineChanges()?.length ?? 0);
    });
  };

  return (
    <Sheet
      open={schemaUpdateActive}
      onOpenChange={(open: boolean) => !open && closeUpdateSchemaDrawer()}
    >
      <SheetContent className={"w-full max-w-4xl!"}>
        <SheetHeader className="border-b">Modifier {schemaName}</SheetHeader>
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
          <UpdateSchemaActions
            changeCount={changeCount}
            valueRef={editorValueRef}
            schemaName={schemaName}
            jsonError={jsonError}
          />
        </div>
      </SheetContent>
    </Sheet>
  );
};

type UpdateSchemaActions = {
  changeCount: number;
  valueRef: React.RefObject<string>;
  schemaName: string;
  jsonError: string | null;
};
const UpdateSchemaActions = ({
  changeCount,
  valueRef,
  schemaName,
  jsonError,
}: UpdateSchemaActions) => {
  const [commitMessage, setCommitMessage] = useState("");
  const { ref: branch } = rootRouteApi.useSearch();
  const newBranch = `mirror-${defaultWorkingBranch}/${branch}`;
  const router = useRouter();

  const { mutateAsync, isPending } = useUpdateSchema();

  const handleSubmit = () => {
    const value = valueRef.current;

    toast.promise(
      mutateAsync({
        body: {
          data: value,
          ref: newBranch,
          commit_message: commitMessage,
          new_branch: null,
        },
        schemaId: schemaName,
      }).then(async (res) => {
        await router.invalidate();
        return res;
      }),
      {
        loading: "Mise à jour du schéma…",
        success: (data) => ({
          title: "Schéma mis à jour.",
          description: <a href={data.commit_url}>{data.commit_url}</a>,
        }),
        error: (err) => err.message ?? "Échec de la mise à jour.",
      },
    );
  };

  return (
    <div className="flex flex-col gap-6">
      {jsonError && (
        <Alert variant="destructive">
          <TriangleAlert />
          <AlertTitle>JSON invalide</AlertTitle>
          <AlertDescription>{jsonError}</AlertDescription>
        </Alert>
      )}

      {changeCount > 0 && (
        <>
          <p>
            Ce commit apportera{" "}
            <b>
              {changeCount} changement{changeCount > 1 && "s"}{" "}
            </b>
            sur la branche
            <span className="bg-muted rounded px-1">{newBranch}</span>
          </p>
          <Alert className="bg-yellow-400/10">
            <TriangleAlert />
            <AlertDescription>
              La branche{" "}
              <span className="bg-muted rounded px-1">{newBranch}</span> doit
              exister sur le repo distant.
            </AlertDescription>
          </Alert>
        </>
      )}

      <textarea
        className="w-full min-h-16 rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        placeholder="Message de commit"
        value={commitMessage}
        onChange={(e) => setCommitMessage(e.target.value)}
      />
      <div className="flex gap-2 justify-between items-center">
        <Button
          disabled={
            !changeCount ||
            !commitMessage ||
            !branch ||
            isPending ||
            !!jsonError
          }
          onClick={handleSubmit}
        >
          {isPending ? "Commiting..." : "Commit"}
        </Button>
      </div>
    </div>
  );
};
