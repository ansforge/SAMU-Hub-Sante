const express = require('express');
const octokit = require('octokit');
const config = require('../config');

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

const VALIDATION_ERROR_MESSAGE = 'Missing mandatory attribute in payload';

const validatePayload = (body) => {
  if (!body.fileName) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: fileName (name of the file to update)`);
  }
  if (!body.content) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: content (content of the file to update)`);
  }
  if (!body.branchConfig) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig`);
  }
  if (!body.branchConfig.branch) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig.branch (branch to update)`);
  }
  if (body.branchConfig.isNewBranch === undefined) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig.isNewBranch (branch to update)`);
  }
  if (body.branchConfig.isNewBranch && !body.branchConfig.newBranch) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig.newBranch (required because branchConfig.isNewBranch is set to true)`);
  }
};

const commitModelesChanges = async (req, res) => {
  const {
    fileName,
    content,
    branchConfig,
    password,
  } = req.body;

  try {
    validatePayload(req.body);
  } catch (err) {
    res.status(403).json({ message: err.message });
    return;
  }

  if (password !== config.ADMIN_PASSWORD) {
    res.status(404).json({ message: 'Unauthorized' });
    return;
  }

  if (branchConfig.isNewBranch) {
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
