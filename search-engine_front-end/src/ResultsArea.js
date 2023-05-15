import React from 'react';
import './ResultsArea.css';
import ResultItem from './ResultItem';
import { Pagination } from '@mui/material';

let websiteInfo = [
  {
    id: 0,
    title: "Wikipedia",
    link: "https://www.wikipedia.org/",
    paragraph: "Wikipedia is a free online encyclopedia, created and edited by volunteers around the world and hosted by the Wikimedia Foundation.",
    searchedWords: ["Wikipedia"]
  },
  {
    id: 1,
    title: "Facebook",
    link: "https://www.facebook.com/",
    paragraph: "Create an account or log into Facebook. Connect with friends, family and other people you know. Share photos and videos, send messages and get updates.",
    searchedWords: ["Facebook"]
  },
  {
    id: 2,
    title: "Google",
    link: "https://www.google.com/",
    paragraph: "Search the world's information, including webpages, images, videos and more. Google has many special features to help you find exactly what you're looking ...",
    searchedWords: ["Google"]
  },
  {
    id: 3,
    title:"GeeksforGeeks",
    link:"https://www.geeksforgeeks.org",
    paragraph:"GeeksforGeeks | A computer science portal for geeks Projects Hello, What Do You Want To Learn? DSA: Basic To Advanced Course GATE CS 2024: LIVE Classes Complete Data ...",
    searchedWords: ["geeksforgeeks"]
    
  },
  {
    id: 4,
    title: "Twitter",
    link: "https://twitter.com/",
    paragraph: "From breaking news and entertainment to sports and politics, get the full story with all the live commentary.",
    searchedWords: ["Twitter"]
  },
  {
    id: 5,
    title: "Pinterest",
    link: "https://www.pinterest.com/",
    paragraph: "Discover recipes, home ideas, style inspiration and other ideas to try.",
    searchedWords: ["Pinterset"]
  },
  {
    id: 6,
    title: "HackerRank",
    link: "https://www.hackerrank.com",
    paragraph:"HackerRank is the market-leading technical assessment and remote interview solution for hiring developers. Start hiring at the pace of innovation!",
    searchedWords: ["hackerrank","technical assesment"]
  },
  {
    id: 7,
    title: "LeetCode",
    link: "https://leetcode.com/",
    paragraph: "LeetCode is the best platform to help you enhance your skills, expand your knowledge and prepare for technical interviewsLeetCode is the best platform to help you enhance your skills, expand your knowledge and prepare for technical interviews",
    searchedWords: ["Leetcode"]
  },
  {
    id: 8,
    title: "Tumblr",
    link: "https://www.tumblr.com/",
    paragraph: "Explore trending topics on Tumblr. See all of the GIFs, fan art, and general conversation about the internet's favorite things.",
    searchedWords: ["Tumblr"]
  },
  {
    id: 9,
    title: "GitHub",
    link: "https://github.com",
    paragraph:"GitHub is where over 100 million developers shape the future of software, together. Contribute to the open source community, manage your Git repositories, review code like …",
    searchedWords: ["github","repositories"]
  },
  {
    id: 10,
    title: "Cairo University",
    link: "https://eng.cu.edu.eg/en/",
    paragraph: "Latest Tweets. @CairoUniv: Cairo University Launches 4 Research Projects in Oncology, Biotechnology, Nanotechnology, Electrical Power with… https://t.co ...",
    searchedWords: ["Cairo", "University"]
  }
];

function createResultItems(Result) { 
  return (
    <ResultItem
      key={Result.id}
      title={Result.title}
      link={Result.link}
      paragraph={Result.paragraph}
      searchedWords={Result.searchedWords} 
      />
  );
}


function ResultsArea(props) {
    const [allResults, setAllResults] = React.useState(websiteInfo);
    const [results, setResults] = React.useState([]);
    const [currPage, setCurrPage] = React.useState(1);

    
    function handlePageClick(event,value) { 
        setCurrPage(value);
    }
    React.useEffect(
        () => { 
            const begin = (currPage - 1) * 10;
            const end = begin + 10;
            setResults(allResults.slice(begin, end));
        }
        , [currPage])

    return (
    <div className='results'>
          {results.map(createResultItems)}
            <div className='pagination-div'>
                <Pagination
                    count={Math.ceil(allResults.length/10)}
                    page={currPage} // current page
                    onChange={handlePageClick}
                    variant="outlined"
                    color='secondary'
                />
            </div>
    </div>
      
      
      
  );
}

export default ResultsArea;