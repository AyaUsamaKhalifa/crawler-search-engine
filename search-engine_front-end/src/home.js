import React from 'react';
import './home.css';
import Image from "./images/Search2.png"
import SearchArea from './SearchArea';



function Home() {

  return (
      <div className = "searchArea">
        <img className = "imageFeatures" src = {Image} alt='Search'/>
          <SearchArea />
      </div>
  );
}

export default Home;