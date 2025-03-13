const octokit = require('octokit');

const client = new octokit.Octokit({ auth: process.env.GITHUB_TOKEN });

const GITHUB_OWNER = 'ansforge';
const GITHUB_REPO = 'SAMU-Hub-Modeles';
const EXAMPLE_FILES_PATH = 'src/main/resources/sample/examples';
const DEFAULT_PR_TITLE = 'JSON Creator Examples Update';
const DEFAULT_PR_DESCRIPTION = 'PR opened automatically through the JSON Creator UI.';
const GITHUB_TOKEN_USER = 'ansforge';

const generateCommitMessage = (fileName) => `Update of the json example ${fileName}`;

const createNewBranch = async ({ baseBranch, newBranch }) => {
  const baseBranchCommit = await client.rest.repos.getCommit({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
    ref: baseBranch,
  });

  const baseCommitSha = baseBranchCommit.data.sha;

  await client.rest.git.createRef({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
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
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
    path: filePath,
    ref: branch,
  });
  const fileSha = fileShaResponse.data.sha;

  const encodedContent = Buffer.from(content).toString('base64');

  const response = await client.rest.repos.createOrUpdateFileContents({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
    path: filePath,
    message: generateCommitMessage(fileName),
    content: encodedContent,
    sha: fileSha,
    branch,
  });

  return response.data;
};

const getModelesBranchNames = async () => {
  const response = await client.rest.repos.listBranches({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
  });
  return response.data.map(({ name }) => name);
};

const createPullRequest = async ({ baseBranch, headBranch }) => {
  const response = await client.rest.pulls.create({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
    base: baseBranch,
    head: headBranch,
    title: DEFAULT_PR_TITLE,
    body: DEFAULT_PR_DESCRIPTION,
  });
  return response.data;
};

const findExistingPullRequest = async ({ baseBranch, headBranch }) => {
  const response = await client.rest.pulls.list({
    owner: GITHUB_OWNER,
    repo: GITHUB_REPO,
    state: 'open',
    head: `${GITHUB_TOKEN_USER}:${headBranch}`,
    base: baseBranch,
  });
  return response.data;
};

module.exports = {
  createNewBranch,
  commitModelesChangesToExistingBranch,
  getModelesBranchNames,
  createPullRequest,
  findExistingPullRequest,
};
