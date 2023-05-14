import React from 'react';
import './ResultsPage.css';
import Image from "./images/Search2.png"
import SearchArea from './SearchArea'
import ResultsArea from './ResultsArea';
import { Link } from 'react-router-dom';

function ResultsPage() {

  return (
        <div className='results-page'>
          <div className='search-section'>
              <Link to = "/" className='link'>
                <img className="image-features" src={Image} alt='Search' />
              </Link>
              <div className='text-area'>
                  <SearchArea />
              </div>
          </div>
          <ResultsArea />
        </div>
  );
}

export default ResultsPage;