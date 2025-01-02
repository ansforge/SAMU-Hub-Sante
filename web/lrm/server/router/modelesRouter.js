const express = require('express');
const octokit = require('octokit');

const client = new octokit.Octokit({ auth: process.env.GITHUB_TOKEN });

const OWNER = 'ansforge';
const REPO = 'SAMU-Hub-Modeles';
const EXAMPLE_FILES_PATH = 'src/main/resources/sample/examples';

const getModelesBranchesHandler = async (_, res) => {
  const response = await client.rest.repos.listBranches({
    owner: OWNER,
    repo: REPO,
  });
  res.status(200).json(response.data.map(({ name }) => name));
};

const generateCommitMessage = (fileName) => `Update of the json example ${fileName}`;

const createNewBranch = async ({
  baseBranch,
  newBranch,
}) => {
  const baseBranchCommit = await client.rest.repos.getCommit({
    owner: OWNER,
    repo: REPO,
    ref: baseBranch,
  });

  const baseCommitSha = baseBranchCommit.data.sha;

  await client.rest.git.createRef({
    owner: OWNER,
    repo: REPO,
    ref: `refs/heads/${newBranch}`,
    sha: baseCommitSha,
  });
};

const commitModelesChangesToExistingBranch = async ({
  branch,
  fileName,
  content,
}) => {
  const filePath = `${EXAMPLE_FILES_PATH}/${fileName}`;

  const fileShaResponse = await client.rest.repos.getContent({
    owner: OWNER,
    repo: REPO,
    path: filePath,
    ref: branch,
  });
  const fileSha = fileShaResponse.data.sha;

  const encodedContent = Buffer.from(content).toString('base64');

  const response = await client.rest.repos.createOrUpdateFileContents({
    owner: OWNER,
    repo: REPO,
    path: filePath,
    message: generateCommitMessage(fileName),
    content: encodedContent,
    sha: fileSha,
    branch,
  });

  return response.data;
};

const commitModelesChanges = async (req, res) => {
  const {
    fileName,
    content,
    branchConfig,
  } = req.body;

  if (branchConfig.isNewBranch) {
    if (!branchConfig.baseBranch) {
      res.json(403).send({ message: 'Missing branchConfig.baseBranch in payload' });
    }
    await createNewBranch({
      baseBranch: branchConfig.baseBranch,
      newBranch: branchConfig.branch,
    });
  }

  const result = await commitModelesChangesToExistingBranch({
    branch: branchConfig.branch,
    fileName,
    content,
  });

  res.status(200).json({ message: 'Commit created', data: result });
};

const ModelesRouter = express.Router();

ModelesRouter.get('/branches', getModelesBranchesHandler);

ModelesRouter.post('/', commitModelesChanges);

module.exports = ModelesRouter;
