const express = require('express');
const octokit = require('octokit');

const client = new octokit.Octokit({ auth: process.env.GITHUB_TOKEN });

const getModelesBranchesHandler = async (_, res) => {
  const response = await client.rest.repos.listBranches({
    owner: 'ansforge',
    repo: 'SAMU-Hub-Modeles',
  });
  res.status(200).json(response.data.map(({ name }) => name));
};

const ModelesRouter = express.Router();

ModelesRouter.get('/branches', getModelesBranchesHandler);

module.exports = ModelesRouter;
