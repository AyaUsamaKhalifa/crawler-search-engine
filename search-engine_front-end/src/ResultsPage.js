import React from "react";
import "./ResultsPage.css";
import Image from "./images/Search2.png";
import SearchArea from "./SearchArea";
import ResultsArea from "./ResultsArea";
import { Link } from "react-router-dom";
import { useParams } from "react-router-dom";
import axios from "axios";

function ResultsPage() {
  const [searchResult, setSearchResults] = React.useState([]);
  const [timer, setTimer] = React.useState(0);
  let { SearchResult } = useParams();
  React.useEffect(() => {
    const start = performance.now();
    (async () => {
      await axios.get(`http://localhost:8081/SearchResults/${encodeURIComponent(SearchResult)}`)
        .then((response) => {
          setSearchResults(response.data);
          const end = performance.now();
          setTimer(end - start);
          //console.log(response.data);
        })
        .catch((error) => {
          console.error(error);
        });
    })();
  }, [SearchResult]);
  return (
    <div className="results-page">
      <div className="search-section">
        <Link to="/" className="link">
          <img className="image-features" src={Image} alt="Search" />
        </Link>
          <SearchArea SearchResults={searchResult} />
      </div>
      <div className="timer">The search took {timer/1000} seconds</div>
      <ResultsArea websiteInfo={searchResult} />
    </div>
  );
}

export default ResultsPage;
